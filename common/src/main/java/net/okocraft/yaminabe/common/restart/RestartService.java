package net.okocraft.yaminabe.common.restart;

import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class RestartService implements AutoCloseable {

    private final Scheduler scheduler;
    private final Clock clock;
    private final Listener listener;
    private final AtomicReference<ActiveReservation> current = new AtomicReference<>();

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

        while (true) {
            previous = this.current.get();
            if (reservation.source() == ReservationSource.AUTOMATIC
                && previous != null
                && previous.reservation.source() == ReservationSource.MANUAL) {
                return new ScheduleResult(ScheduleStatus.REJECTED_BY_MANUAL, null);
            }
            if (this.current.compareAndSet(previous, next)) {
                break;
            }
        }

        if (previous != null) {
            previous.cancel();
            this.listener.onCancelled(previous.reservation);
        }

        try {
            this.arm(next);
        } catch (RuntimeException | Error exception) {
            if (this.current.compareAndSet(next, null)) {
                next.cancel();
                this.listener.onCancelled(next.reservation);
            } else {
                next.cancel();
            }
            throw exception;
        }

        return new ScheduleResult(
            previous == null ? ScheduleStatus.SCHEDULED : ScheduleStatus.REPLACED,
            previous == null ? null : previous.reservation
        );
    }

    public Optional<Snapshot> current() {
        ActiveReservation active = this.current.get();
        return active == null ? Optional.empty() : Optional.of(new Snapshot(active.reservation, active.phase.get()));
    }

    public Optional<ShutdownReservation> cancel() {
        while (true) {
            ActiveReservation active = this.current.get();
            if (active == null) {
                return Optional.empty();
            }
            if (this.current.compareAndSet(active, null)) {
                active.cancel();
                this.listener.onCancelled(active.reservation);
                return Optional.of(active.reservation);
            }
        }
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
        if (this.current.get() != active || !active.startCountdown()) {
            return;
        }
        this.listener.onCountdownStarted(active.reservation);
    }

    private void execute(ActiveReservation active) {
        if (!this.current.compareAndSet(active, null) || !active.beginExecution()) {
            return;
        }
        this.listener.onExecute(active.reservation);
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
            if ((status == ScheduleStatus.REPLACED) != (replaced != null)) {
                throw new IllegalArgumentException("replaced reservation must be present only for REPLACED status");
            }
        }

        public boolean scheduled() {
            return this.status != ScheduleStatus.REJECTED_BY_MANUAL;
        }
    }

    private static final class ActiveReservation {

        private final ShutdownReservation reservation;
        private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.WAITING);
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final AtomicReference<CancellableTask> executionTask = new AtomicReference<>();
        private final AtomicReference<CancellableTask> countdownTask = new AtomicReference<>();

        private ActiveReservation(ShutdownReservation reservation) {
            this.reservation = reservation;
        }

        private void setExecutionTask(CancellableTask task) {
            setTask(this.executionTask, task, this.active);
        }

        private void setCountdownTask(CancellableTask task) {
            setTask(this.countdownTask, task, this.active);
        }

        private boolean startCountdown() {
            return this.active.get() && this.phase.compareAndSet(Phase.WAITING, Phase.COUNTDOWN);
        }

        private boolean beginExecution() {
            if (!this.active.compareAndSet(true, false)) {
                return false;
            }
            cancel(this.countdownTask.get());
            return true;
        }

        private void cancel() {
            if (!this.active.compareAndSet(true, false)) {
                return;
            }
            cancel(this.executionTask.get());
            cancel(this.countdownTask.get());
        }

        private static void setTask(AtomicReference<CancellableTask> reference, CancellableTask task, AtomicBoolean active) {
            Objects.requireNonNull(task);
            if (!reference.compareAndSet(null, task)) {
                task.cancel();
                throw new IllegalStateException("task is already set");
            }
            if (!active.get()) {
                task.cancel();
            }
        }

        private static void cancel(@Nullable CancellableTask task) {
            if (task != null) {
                task.cancel();
            }
        }
    }
}
