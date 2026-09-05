package net.okocraft.yaminabe.common.restart;

import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class RestartServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");

    @Test
    void testManualReservationReplacesAutomaticReservation() {
        TestScheduler scheduler = new TestScheduler();
        RecordingListener listener = new RecordingListener();
        RestartService service = service(scheduler, listener);
        ShutdownReservation automatic = reservation(ReservationSource.AUTOMATIC, Duration.ofHours(1));
        ShutdownReservation manual = reservation(ReservationSource.MANUAL, Duration.ofHours(2));

        service.schedule(automatic);
        RestartService.ScheduleResult result = service.schedule(manual);

        Assertions.assertEquals(RestartService.ScheduleStatus.REPLACED, result.status());
        Assertions.assertEquals(automatic, result.replaced());
        Assertions.assertEquals(manual, service.current().orElseThrow().reservation());
        Assertions.assertTrue(scheduler.tasks.get(0).cancelled);
        Assertions.assertTrue(scheduler.tasks.get(1).cancelled);
        Assertions.assertEquals(List.of(automatic), listener.cancelled);
    }

    @Test
    void testAutomaticReservationDoesNotReplaceManualReservation() {
        TestScheduler scheduler = new TestScheduler();
        RecordingListener listener = new RecordingListener();
        RestartService service = service(scheduler, listener);
        ShutdownReservation manual = reservation(ReservationSource.MANUAL, Duration.ofHours(1));
        ShutdownReservation automatic = reservation(ReservationSource.AUTOMATIC, Duration.ofHours(2));

        service.schedule(manual);
        RestartService.ScheduleResult result = service.schedule(automatic);

        Assertions.assertEquals(RestartService.ScheduleStatus.REJECTED_BY_MANUAL, result.status());
        Assertions.assertFalse(result.scheduled());
        Assertions.assertEquals(manual, service.current().orElseThrow().reservation());
        Assertions.assertEquals(2, scheduler.tasks.size());
    }

    @Test
    void testCountdownChangesCurrentPhase() {
        TestScheduler scheduler = new TestScheduler();
        RecordingListener listener = new RecordingListener();
        RestartService service = service(scheduler, listener);
        ShutdownReservation reservation = reservation(ReservationSource.MANUAL, Duration.ofMinutes(10));

        service.schedule(reservation);
        Assertions.assertEquals(RestartService.Phase.WAITING, service.current().orElseThrow().phase());

        scheduler.tasks.get(1).run();

        Assertions.assertEquals(RestartService.Phase.COUNTDOWN, service.current().orElseThrow().phase());
        Assertions.assertEquals(List.of(reservation), listener.countdownStarted);
    }

    @Test
    void testCancelCancelsScheduledTasks() {
        TestScheduler scheduler = new TestScheduler();
        RecordingListener listener = new RecordingListener();
        RestartService service = service(scheduler, listener);
        ShutdownReservation reservation = reservation(ReservationSource.MANUAL, Duration.ofMinutes(10));

        service.schedule(reservation);
        Assertions.assertEquals(reservation, service.cancel().orElseThrow());

        Assertions.assertTrue(service.current().isEmpty());
        Assertions.assertTrue(scheduler.tasks.get(0).cancelled);
        Assertions.assertTrue(scheduler.tasks.get(1).cancelled);
        Assertions.assertEquals(List.of(reservation), listener.cancelled);
    }

    @Test
    void testExecutionWinsOverLaterCancellation() {
        TestScheduler scheduler = new TestScheduler();
        RecordingListener listener = new RecordingListener();
        RestartService service = service(scheduler, listener);
        ShutdownReservation reservation = reservation(ReservationSource.MANUAL, Duration.ofMinutes(10));

        service.schedule(reservation);
        scheduler.tasks.get(0).run();

        Assertions.assertEquals(List.of(reservation), listener.executed);
        Assertions.assertTrue(service.current().isEmpty());
        Assertions.assertTrue(service.cancel().isEmpty());
        Assertions.assertTrue(scheduler.tasks.get(1).cancelled);
        Assertions.assertTrue(listener.cancelled.isEmpty());
    }

    private static RestartService service(TestScheduler scheduler, RecordingListener listener) {
        return new RestartService(scheduler, Clock.fixed(NOW, ZoneOffset.UTC), listener);
    }

    private static ShutdownReservation reservation(ReservationSource source, Duration delay) {
        return ShutdownReservation.create(
            NOW,
            NOW.plus(delay),
            Duration.ofSeconds(60),
            ShutdownType.RESTART,
            source,
            null
        );
    }

    private static final class RecordingListener implements RestartService.Listener {

        private final List<ShutdownReservation> countdownStarted = new ArrayList<>();
        private final List<ShutdownReservation> cancelled = new ArrayList<>();
        private final List<ShutdownReservation> executed = new ArrayList<>();

        @Override
        public void onCountdownStarted(ShutdownReservation reservation) {
            this.countdownStarted.add(reservation);
        }

        @Override
        public void onCancelled(ShutdownReservation reservation) {
            this.cancelled.add(reservation);
        }

        @Override
        public void onExecute(ShutdownReservation reservation) {
            this.executed.add(reservation);
        }
    }

    private static final class TestScheduler implements Scheduler {

        private final List<TestTask> tasks = new ArrayList<>();

        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            TestTask testTask = new TestTask(task, delay);
            this.tasks.add(testTask);
            return testTask;
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(
            @NotNull Consumer<CancellableTask> task,
            @NotNull Duration interval
        ) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestTask implements CancellableTask {

        private final Runnable runnable;
        @SuppressWarnings("unused")
        private final Duration delay;
        private boolean cancelled;

        private TestTask(Runnable runnable, Duration delay) {
            this.runnable = runnable;
            this.delay = delay;
        }

        private void run() {
            if (!this.cancelled) {
                this.runnable.run();
            }
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }
    }
}
