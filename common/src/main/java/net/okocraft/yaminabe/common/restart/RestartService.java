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

        synchronized (this.stateLock) {
            ActiveReservation previous = this.current;
            if (reservation.source() == ReservationSource.AUTOMATIC
                && previous != null
                && previous.reservation.source() == ReservationSource.MANUAL) {
                return new ScheduleResult(ScheduleStatus.REJECTED_BY_MANUAL, null);
            }

            this.current = next;
            if (previous != null) {
                previous.cancel();
                try {
                    this.listener.onCancelled(previous.reservation);
                } catch (RuntimeException | Error exception) {
                    if (this.current == next) {
                        this.current = null;
                    }
                    next.cancel();
                    throw exception;
                }
            }

            try {
                this.arm(next);
            } catch (RuntimeException | Error exception) {
                if (this.current == next) {
                    this.current = null;
                    next.cancel();
                    try {
                        this.listener.onCancelled(next.reservation);
                    } catch (RuntimeException | Error listenerException) {
                        exception.addSuppressed(listenerException);
                    }
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
    }

    public Optional<Snapshot> current() {
        synchronized (this.stateLock) {
            ActiveReservation active = this.current;
            return active == null ? Optional.empty() : Optional.of(new Snapshot(active.reservation, active.phase));
        }
    }

    public Optional<ShutdownReservation> cancel() {
        synchronized (this.stateLock) {
            ActiveReservation active = this.current;
            if (active == null) {
                return Optional.empty();
            }

            this.current = null;
            active.cancel();
            this.listener.onCancelled(active.reservation);
            return Optional.of(active.reservation);
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
        synchronized (this.stateLock) {
            if (this.current != active || !active.startCountdown()) {
                return;
            }
            this.listener.onCountdownStarted(active.reservation);
        }
    }

    private void execute(ActiveReservation active) {
        synchronized (this.stateLock) {
            if (this.current != active || !active.beginExecution()) {
                return;
            }
            this.current = null;
            this.listener.onExecute(active.reservation);
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
        private Phase phase = Phase.WAITING;
        private boolean active = true;
        private @Nullable CancellableTask executionTask;
        private @Nullable CancellableTask countdownTask;

        private ActiveReservation(ShutdownReservation reservation) {
            this.reservation = reservation;
        }

        private void setExecutionTask(CancellableTask task) {
            Objects.requireNonNull(task);
            if (this.executionTask != null) {
                task.cancel();
                throw new IllegalStateException("execution task is already set");
            }
            this.executionTask = task;
            if (!this.active) {
                task.cancel();
            }
        }

        private void setCountdownTask(CancellableTask task) {
            Objects.requireNonNull(task);
            if (this.countdownTask != null) {
                task.cancel();
                throw new IllegalStateException("countdown task is already set");
            }
            this.countdownTask = task;
            if (!this.active) {
                task.cancel();
            }
        }

        private boolean startCountdown() {
            if (!this.active || this.phase != Phase.WAITING) {
                return false;
            }
            this.phase = Phase.COUNTDOWN;
            return true;
        }

        private boolean beginExecution() {
            if (!this.active) {
                return false;
            }
            this.active = false;
            cancel(this.countdownTask);
            return true;
        }

        private void cancel() {
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
