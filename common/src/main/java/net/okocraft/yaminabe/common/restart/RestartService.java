package net.okocraft.yaminabe.common.restart;

import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class RestartService implements AutoCloseable {

    private final Scheduler scheduler;
    private final Clock clock;
    private final Listener listener;
    private final Object stateLock = new Object();
    private @Nullable ActiveReservation current;

    public RestartService(Scheduler scheduler, Clock clock, Listener listener) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.clock = Objects.requireNonNull(clock);
        this.listener = Objects.requireNonNull(listener);
    }

    public RestartService(Scheduler scheduler, Listener listener) {
        this(scheduler, Clock.systemUTC(), listener);
    }

    public ScheduleResult schedule(ShutdownReservation reservation) {
        Objects.requireNonNull(reservation);
        ActiveReservation next = new ActiveReservation(reservation);
        ActiveReservation previous;
        @Nullable TerminalNotification cancellation;

        synchronized (this.stateLock) {
            previous = this.current;
            if (reservation.source() == ReservationSource.AUTOMATIC
                && previous != null
                && previous.reservation.source() == ReservationSource.MANUAL) {
                return new ScheduleResult(ScheduleStatus.REJECTED_BY_MANUAL, null);
            }

            this.current = next;
            try {
                this.arm(next);
            } catch (RuntimeException | Error exception) {
                if (this.current == next) {
                    this.current = previous;
                }
                next.discard();
                throw exception;
            }
            cancellation = previous == null ? null : previous.cancel();
        }

        if (cancellation != null) {
            this.notifyTerminal(previous.reservation, cancellation);
        }

        synchronized (this.stateLock) {
            if (this.current != next) {
                return new ScheduleResult(
                    ScheduleStatus.SUPERSEDED,
                    previous == null ? null : previous.reservation
                );
            }
        }
        return new ScheduleResult(
            previous == null ? ScheduleStatus.SCHEDULED : ScheduleStatus.REPLACED,
            previous == null ? null : previous.reservation
        );
    }

    public Optional<Snapshot> current() {
        synchronized (this.stateLock) {
            ActiveReservation active = this.current;
            return active == null ? Optional.empty() : Optional.of(new Snapshot(active.reservation, active.phase()));
        }
    }

    public Optional<ShutdownReservation> cancel() {
        ActiveReservation active;
        @Nullable TerminalNotification notification;
        synchronized (this.stateLock) {
            active = this.current;
            if (active == null) {
                return Optional.empty();
            }
            this.current = null;
            notification = active.cancel();
        }

        if (notification != null) {
            this.notifyTerminal(active.reservation, notification);
        }
        return Optional.of(active.reservation);
    }

    @Override
    public void close() {
        this.cancel();
    }

    private void arm(ActiveReservation active) {
        Instant now = this.clock.instant();
        active.setExecutionTask(this.scheduler.runDelayed(
            () -> this.execute(active),
            delayUntil(now, active.reservation.executeAt())
        ));
        active.setCountdownTask(this.scheduler.runDelayed(
            () -> this.startCountdown(active),
            delayUntil(now, active.reservation.countdownStartAt())
        ));
    }

    private void startCountdown(ActiveReservation active) {
        synchronized (this.stateLock) {
            if (this.current != active || !active.startCountdown()) {
                return;
            }
        }

        Throwable failure = null;
        try {
            this.listener.onCountdownStarted(active.reservation);
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            TerminalNotification deferred;
            synchronized (this.stateLock) {
                deferred = active.finishCountdownNotification();
            }
            if (deferred != null) {
                if (failure == null) {
                    this.notifyTerminal(active.reservation, deferred);
                } else {
                    notifyTerminal(this.listener, active.reservation, deferred, failure);
                }
            }
        }
    }

    private void execute(ActiveReservation active) {
        @Nullable TerminalNotification notification;
        synchronized (this.stateLock) {
            if (this.current != active) {
                return;
            }
            this.current = null;
            notification = active.execute();
        }

        if (notification != null) {
            this.notifyTerminal(active.reservation, notification);
        }
    }

    private void notifyTerminal(ShutdownReservation reservation, TerminalNotification notification) {
        switch (notification) {
            case CANCELLED -> this.listener.onCancelled(reservation);
            case EXECUTE -> this.listener.onExecute(reservation);
        }
    }

    private static void notifyTerminal(
        Listener listener,
        ShutdownReservation reservation,
        TerminalNotification notification,
        Throwable failure
    ) {
        try {
            switch (notification) {
                case CANCELLED -> listener.onCancelled(reservation);
                case EXECUTE -> listener.onExecute(reservation);
            }
        } catch (RuntimeException | Error listenerException) {
            failure.addSuppressed(listenerException);
        }
    }

    private static Duration delayUntil(Instant now, Instant target) {
        return target.isAfter(now) ? Duration.between(now, target) : Duration.ZERO;
    }

    public interface Listener {

        default void onCountdownStarted(ShutdownReservation reservation) {
        }

        default void onCancelled(ShutdownReservation reservation) {
        }

        default void onExecute(ShutdownReservation reservation) {
        }
    }

    public enum Phase {
        WAITING,
        COUNTDOWN
    }

    public enum ScheduleStatus {
        SCHEDULED,
        REPLACED,
        SUPERSEDED,
        REJECTED_BY_MANUAL
    }

    public record Snapshot(ShutdownReservation reservation, Phase phase) {

        public Snapshot {
            Objects.requireNonNull(reservation);
            Objects.requireNonNull(phase);
        }
    }

    public record ScheduleResult(ScheduleStatus status, @Nullable ShutdownReservation replaced) {

        public ScheduleResult {
            Objects.requireNonNull(status);
            if (status == ScheduleStatus.REPLACED && replaced == null) {
                throw new IllegalArgumentException("replaced reservation must be present for REPLACED status");
            }
            if ((status == ScheduleStatus.SCHEDULED || status == ScheduleStatus.REJECTED_BY_MANUAL)
                && replaced != null) {
                throw new IllegalArgumentException("replaced reservation must be absent for this status");
            }
        }

        public boolean scheduled() {
            return this.status == ScheduleStatus.SCHEDULED || this.status == ScheduleStatus.REPLACED;
        }
    }

    private enum State {
        WAITING,
        COUNTDOWN_NOTIFYING,
        COUNTDOWN,
        DONE
    }

    private enum TerminalNotification {
        CANCELLED,
        EXECUTE
    }

    private static final class ActiveReservation {

        private final ShutdownReservation reservation;
        private State state = State.WAITING;
        private @Nullable CancellableTask executionTask;
        private @Nullable CancellableTask countdownTask;
        private @Nullable TerminalNotification deferredTerminalNotification;

        private ActiveReservation(ShutdownReservation reservation) {
            this.reservation = reservation;
        }

        private Phase phase() {
            return switch (this.state) {
                case WAITING -> Phase.WAITING;
                case COUNTDOWN_NOTIFYING, COUNTDOWN -> Phase.COUNTDOWN;
                case DONE -> throw new IllegalStateException("completed reservation cannot be current");
            };
        }

        private void setExecutionTask(CancellableTask task) {
            this.executionTask = this.setTask(this.executionTask, task);
        }

        private void setCountdownTask(CancellableTask task) {
            this.countdownTask = this.setTask(this.countdownTask, task);
        }

        private CancellableTask setTask(@Nullable CancellableTask existing, CancellableTask task) {
            Objects.requireNonNull(task);
            if (existing != null) {
                task.cancel();
                throw new IllegalStateException("task is already set");
            }
            if (this.state == State.DONE) {
                task.cancel();
            }
            return task;
        }

        private boolean startCountdown() {
            if (this.state != State.WAITING) {
                return false;
            }
            this.state = State.COUNTDOWN_NOTIFYING;
            return true;
        }

        private @Nullable TerminalNotification finishCountdownNotification() {
            if (this.state == State.COUNTDOWN_NOTIFYING) {
                this.state = State.COUNTDOWN;
            }
            TerminalNotification deferred = this.deferredTerminalNotification;
            this.deferredTerminalNotification = null;
            return deferred;
        }

        private @Nullable TerminalNotification cancel() {
            State previous = this.state;
            if (previous == State.DONE) {
                return null;
            }
            this.state = State.DONE;
            cancel(this.executionTask);
            cancel(this.countdownTask);
            return this.complete(previous, TerminalNotification.CANCELLED);
        }

        private @Nullable TerminalNotification execute() {
            State previous = this.state;
            if (previous == State.DONE) {
                return null;
            }
            this.state = State.DONE;
            cancel(this.countdownTask);
            return this.complete(previous, TerminalNotification.EXECUTE);
        }

        private @Nullable TerminalNotification complete(State previous, TerminalNotification notification) {
            if (previous != State.COUNTDOWN_NOTIFYING) {
                return notification;
            }
            if (this.deferredTerminalNotification != null) {
                throw new IllegalStateException("terminal notification is already deferred");
            }
            this.deferredTerminalNotification = notification;
            return null;
        }

        private void discard() {
            this.state = State.DONE;
            cancel(this.executionTask);
            cancel(this.countdownTask);
        }

        private static void cancel(@Nullable CancellableTask task) {
            if (task != null) {
                task.cancel();
            }
        }
    }
}
