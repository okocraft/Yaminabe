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
        boolean previousNotificationDeferred = false;

        synchronized (this.stateLock) {
            previous = this.current;
            if (reservation.source() == ReservationSource.AUTOMATIC
                && previous != null
                && previous.reservation.source() == ReservationSource.MANUAL) {
                return new ScheduleResult(ScheduleStatus.REJECTED_BY_MANUAL, null);
            }

            this.current = next;
            if (previous != null) {
                previous.cancel();
                previousNotificationDeferred = previous.deferTerminalNotification(TerminalNotification.CANCELLED);
            }
        }

        try {
            this.arm(next);
        } catch (RuntimeException | Error exception) {
            Removal removal = this.removeIfCurrent(next);
            if (previous != null && !previousNotificationDeferred) {
                notifyTerminal(this.listener, previous.reservation, TerminalNotification.CANCELLED, exception);
            }
            if (removal.removed && !removal.notificationDeferred) {
                notifyTerminal(this.listener, next.reservation, TerminalNotification.CANCELLED, exception);
            }
            throw exception;
        }

        if (previous != null && !previousNotificationDeferred) {
            this.listener.onCancelled(previous.reservation);
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
            return active == null ? Optional.empty() : Optional.of(new Snapshot(active.reservation, active.phase));
        }
    }

    public Optional<ShutdownReservation> cancel() {
        ActiveReservation active;
        boolean notificationDeferred;
        synchronized (this.stateLock) {
            active = this.current;
            if (active == null) {
                return Optional.empty();
            }

            this.current = null;
            active.cancel();
            notificationDeferred = active.deferTerminalNotification(TerminalNotification.CANCELLED);
        }

        if (!notificationDeferred) {
            this.listener.onCancelled(active.reservation);
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
        boolean notificationDeferred;
        synchronized (this.stateLock) {
            if (this.current != active || !active.beginExecution()) {
                return;
            }
            this.current = null;
            notificationDeferred = active.deferTerminalNotification(TerminalNotification.EXECUTE);
        }

        if (!notificationDeferred) {
            this.listener.onExecute(active.reservation);
        }
    }

    private Removal removeIfCurrent(ActiveReservation active) {
        synchronized (this.stateLock) {
            if (this.current != active) {
                active.cancel();
                return new Removal(false, false);
            }
            this.current = null;
            active.cancel();
            return new Removal(
                true,
                active.deferTerminalNotification(TerminalNotification.CANCELLED)
            );
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

    private enum TerminalNotification {
        CANCELLED,
        EXECUTE
    }

    private record Removal(boolean removed, boolean notificationDeferred) {
    }

    private static final class ActiveReservation {

        private final ShutdownReservation reservation;
        private Phase phase = Phase.WAITING;
        private boolean active = true;
        private @Nullable CancellableTask executionTask;
        private @Nullable CancellableTask countdownTask;
        private boolean countdownNotificationInProgress;
        private @Nullable TerminalNotification deferredTerminalNotification;

        private ActiveReservation(ShutdownReservation reservation) {
            this.reservation = reservation;
        }

        private synchronized void setExecutionTask(CancellableTask task) {
            this.executionTask = this.setTask(this.executionTask, task);
        }

        private synchronized void setCountdownTask(CancellableTask task) {
            this.countdownTask = this.setTask(this.countdownTask, task);
        }

        private synchronized CancellableTask setTask(@Nullable CancellableTask existing, CancellableTask task) {
            Objects.requireNonNull(task);
            if (existing != null) {
                task.cancel();
                throw new IllegalStateException("task is already set");
            }
            if (!this.active) {
                task.cancel();
            }
            return task;
        }

        private synchronized boolean startCountdown() {
            if (!this.active || this.phase != Phase.WAITING) {
                return false;
            }
            this.phase = Phase.COUNTDOWN;
            this.countdownNotificationInProgress = true;
            return true;
        }

        private synchronized boolean deferTerminalNotification(TerminalNotification notification) {
            if (!this.countdownNotificationInProgress) {
                return false;
            }
            if (this.deferredTerminalNotification != null) {
                throw new IllegalStateException("terminal notification is already deferred");
            }
            this.deferredTerminalNotification = notification;
            return true;
        }

        private synchronized @Nullable TerminalNotification finishCountdownNotification() {
            this.countdownNotificationInProgress = false;
            TerminalNotification deferred = this.deferredTerminalNotification;
            this.deferredTerminalNotification = null;
            return deferred;
        }

        private synchronized boolean beginExecution() {
            if (!this.active) {
                return false;
            }
            this.active = false;
            cancel(this.countdownTask);
            return true;
        }

        private synchronized void cancel() {
            if (!this.active) {
                return;
            }
            this.active = false;
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
