package net.okocraft.yaminabe.common.restart.countdown;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.ReservationSource;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class RestartCountdownPresenterTest {

    private static final Instant NOW = Instant.parse("2026-09-06T03:00:00Z");

    @Test
    void testLateTickBroadcastsCrossedThreshold() {
        MutableClock clock = new MutableClock(NOW);
        TestScheduler scheduler = new TestScheduler();
        Audience audience = Mockito.mock(Audience.class);
        RestartCountdownPresenter presenter = new RestartCountdownPresenter(
            scheduler,
            clock,
            () -> List.of(audience),
            () -> new RestartCountdownSettings(
                false,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS,
                Set.of(5L)
            )
        );
        ShutdownReservation reservation = ShutdownReservation.withFullCountdown(
            NOW,
            NOW.plusSeconds(10),
            ShutdownType.RESTART,
            ReservationSource.MANUAL,
            null
        );

        presenter.start(reservation);
        clock.set(NOW.plusSeconds(4));
        scheduler.task.run(); // 6 seconds remaining
        Mockito.verifyNoInteractions(audience);

        clock.set(NOW.plusSeconds(6));
        scheduler.task.run(); // jumps to 4 seconds remaining and crosses the 5-second threshold

        Mockito.verify(audience).sendMessage(RestartCountdownMessages.countdown(reservation, 5).asComponent());

        clock.set(NOW.plusSeconds(7));
        scheduler.task.run();
        Mockito.verifyNoMoreInteractions(audience);
    }

    private static final class TestScheduler implements Scheduler {
        private TestTask task;

        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(
            @NotNull Consumer<CancellableTask> task,
            @NotNull Duration interval
        ) {
            this.task = new TestTask(task);
            return this.task;
        }
    }

    private static final class TestTask implements CancellableTask {
        private final Consumer<CancellableTask> action;
        private boolean cancelled;

        private TestTask(Consumer<CancellableTask> action) {
            this.action = action;
        }

        private void run() {
            if (!this.cancelled) {
                this.action.accept(this);
            }
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }
    }

    private static final class MutableClock extends Clock {
        private volatile Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void set(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }
}
