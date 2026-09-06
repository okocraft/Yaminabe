package net.okocraft.yaminabe.common.restart;

import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class AutomaticRestartManagerTest {

    private static final Instant NOW = Instant.parse("2026-09-06T03:00:00Z");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    @Test
    void testRefreshSchedulesNextAutomaticRestart() {
        Fixture fixture = fixture();

        fixture.manager.refresh();

        ShutdownReservation reservation = current(fixture.service);
        Assertions.assertEquals(ReservationSource.AUTOMATIC, reservation.source());
        Assertions.assertEquals(ShutdownType.RESTART, reservation.type());
        Assertions.assertEquals(Instant.parse("2026-09-06T09:00:00Z"), reservation.executeAt());
        Assertions.assertEquals(reservation.executeAt().minusSeconds(60), reservation.countdownStartAt());
    }

    @Test
    void testRefreshDoesNotReplaceManualReservation() {
        Fixture fixture = fixture();
        ShutdownReservation manual = ShutdownReservation.create(
            NOW,
            NOW.plus(Duration.ofHours(6)),
            Duration.ofMinutes(5),
            ShutdownType.STOP,
            ReservationSource.MANUAL,
            null
        );
        fixture.service.schedule(manual);

        fixture.manager.refresh();

        Assertions.assertEquals(manual, current(fixture.service));
    }

    @Test
    void testRefreshKeepsMatchingAutomaticReservation() {
        Fixture fixture = fixture();
        fixture.manager.refresh();
        ShutdownReservation original = current(fixture.service);
        int taskCount = fixture.scheduler.tasks.size();

        fixture.manager.refresh();

        Assertions.assertSame(original, current(fixture.service));
        Assertions.assertEquals(taskCount, fixture.scheduler.tasks.size());
    }

    @Test
    void testCancelledManualRearmsFromCurrentTime() {
        TestScheduler scheduler = new TestScheduler();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AtomicReference<AutomaticRestartManager> managerRef = new AtomicReference<>();
        RestartService service = serviceWithCancellationManager(scheduler, clock, managerRef);
        AutomaticRestartManager manager = new AutomaticRestartManager(service, clock, () -> Optional.of(settings()));
        managerRef.set(manager);
        ShutdownReservation manual = ShutdownReservation.create(
            NOW,
            Instant.parse("2026-09-06T20:00:00Z"),
            Duration.ofMinutes(1),
            ShutdownType.RESTART,
            ReservationSource.MANUAL,
            null
        );
        service.schedule(manual);

        service.cancel();

        ShutdownReservation automatic = current(service);
        Assertions.assertEquals(ReservationSource.AUTOMATIC, automatic.source());
        Assertions.assertEquals(Instant.parse("2026-09-06T09:00:00Z"), automatic.executeAt());
    }

    @Test
    void testCancelledAutomaticRearmsAfterCancelledExecutionTime() {
        TestScheduler scheduler = new TestScheduler();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AtomicReference<AutomaticRestartManager> managerRef = new AtomicReference<>();
        RestartService service = serviceWithCancellationManager(scheduler, clock, managerRef);
        AutomaticRestartManager manager = new AutomaticRestartManager(service, clock, () -> Optional.of(settings()));
        managerRef.set(manager);
        ShutdownReservation automatic = ShutdownReservation.create(
            NOW,
            Instant.parse("2026-09-06T20:00:00Z"),
            Duration.ofMinutes(1),
            ShutdownType.RESTART,
            ReservationSource.AUTOMATIC,
            null
        );
        service.schedule(automatic);

        service.cancel();

        ShutdownReservation rearmed = current(service);
        Assertions.assertEquals(ReservationSource.AUTOMATIC, rearmed.source());
        Assertions.assertEquals(Instant.parse("2026-09-07T09:00:00Z"), rearmed.executeAt());
    }

    @Test
    void testDisabledRefreshCancelsAutomaticReservation() {
        TestScheduler scheduler = new TestScheduler();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AtomicReference<Optional<AutomaticRestartManager.Settings>> settings = new AtomicReference<>(Optional.of(settings()));
        RestartService service = new RestartService(scheduler, clock, new RestartService.Listener() {
        });
        AutomaticRestartManager manager = new AutomaticRestartManager(service, clock, settings::get);
        manager.refresh();
        settings.set(Optional.empty());

        manager.refresh();

        Assertions.assertTrue(service.current().isEmpty());
    }

    private static RestartService serviceWithCancellationManager(
        TestScheduler scheduler,
        Clock clock,
        AtomicReference<AutomaticRestartManager> managerRef
    ) {
        return new RestartService(scheduler, clock, new RestartService.Listener() {
            @Override
            public void onCancelled(ShutdownReservation reservation) {
                AutomaticRestartManager manager = managerRef.get();
                if (manager != null) {
                    manager.onCancelled(reservation);
                }
            }
        });
    }

    private static Fixture fixture() {
        TestScheduler scheduler = new TestScheduler();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        RestartService service = new RestartService(scheduler, clock, new RestartService.Listener() {
        });
        AutomaticRestartManager manager = new AutomaticRestartManager(service, clock, () -> Optional.of(settings()));
        return new Fixture(service, manager, scheduler);
    }

    private static AutomaticRestartManager.Settings settings() {
        return new AutomaticRestartManager.Settings(
            new RestartSchedule(TOKYO, List.of(LocalTime.of(18, 0))),
            Duration.ofSeconds(60)
        );
    }

    private static ShutdownReservation current(RestartService service) {
        return service.current().orElseThrow().reservation();
    }

    private record Fixture(RestartService service, AutomaticRestartManager manager, TestScheduler scheduler) {
    }

    private static final class TestScheduler implements Scheduler {
        private final List<TestTask> tasks = new ArrayList<>();

        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            TestTask scheduled = new TestTask();
            this.tasks.add(scheduled);
            return scheduled;
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(
            @NotNull Consumer<CancellableTask> task,
            @NotNull Duration interval
        ) {
            TestTask scheduled = new TestTask();
            this.tasks.add(scheduled);
            return scheduled;
        }
    }

    private static final class TestTask implements CancellableTask {
        @Override
        public void cancel() {
        }
    }
}
