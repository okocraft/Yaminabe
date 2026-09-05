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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

    @Test
    void testCancellationNotificationDoesNotOvertakeCountdownNotification() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        CountDownLatch countdownEntered = new CountDownLatch(1);
        CountDownLatch releaseCountdown = new CountDownLatch(1);
        CountDownLatch cancellationCompleted = new CountDownLatch(1);
        CountDownLatch cancellationNotified = new CountDownLatch(1);
        RestartService.Listener listener = new RestartService.Listener() {
            @Override
            public void onCountdownStarted(ShutdownReservation reservation) {
                countdownEntered.countDown();
                await(releaseCountdown);
            }

            @Override
            public void onCancelled(ShutdownReservation reservation) {
                cancellationNotified.countDown();
            }
        };
        RestartService service = service(scheduler, listener);
        ShutdownReservation reservation = reservation(ReservationSource.MANUAL, Duration.ofMinutes(10));
        service.schedule(reservation);

        Thread countdownThread = daemonThread(() -> scheduler.tasks.get(1).run());
        countdownThread.start();

        try {
            Assertions.assertTrue(countdownEntered.await(1, TimeUnit.SECONDS));
            Thread cancellationThread = daemonThread(() -> {
                service.cancel();
                cancellationCompleted.countDown();
            });
            cancellationThread.start();

            Assertions.assertTrue(cancellationCompleted.await(1, TimeUnit.SECONDS));
            Assertions.assertFalse(cancellationNotified.await(100, TimeUnit.MILLISECONDS));
            Assertions.assertTrue(service.current().isEmpty());
        } finally {
            releaseCountdown.countDown();
        }

        countdownThread.join(1000);
        Assertions.assertFalse(countdownThread.isAlive());
        Assertions.assertTrue(cancellationNotified.await(1, TimeUnit.SECONDS));
    }

    @Test
    void testCancellationListenerFailureDoesNotCorruptReplacement() {
        TestScheduler scheduler = new TestScheduler();
        RestartService.Listener listener = new RestartService.Listener() {
            @Override
            public void onCancelled(ShutdownReservation reservation) {
                throw new IllegalStateException("listener failure");
            }
        };
        RestartService service = service(scheduler, listener);
        ShutdownReservation automatic = reservation(ReservationSource.AUTOMATIC, Duration.ofHours(1));
        ShutdownReservation manual = reservation(ReservationSource.MANUAL, Duration.ofHours(2));

        service.schedule(automatic);
        IllegalStateException exception = Assertions.assertThrows(
            IllegalStateException.class,
            () -> service.schedule(manual)
        );

        Assertions.assertEquals("listener failure", exception.getMessage());
        Assertions.assertEquals(manual, service.current().orElseThrow().reservation());
        Assertions.assertEquals(4, scheduler.tasks.size());
        Assertions.assertTrue(scheduler.tasks.get(0).cancelled);
        Assertions.assertTrue(scheduler.tasks.get(1).cancelled);
        Assertions.assertFalse(scheduler.tasks.get(2).cancelled);
        Assertions.assertFalse(scheduler.tasks.get(3).cancelled);
    }

    @Test
    void testCancellationListenerRunsOutsideStateLock() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        CountDownLatch cancellationEntered = new CountDownLatch(1);
        CountDownLatch releaseCancellation = new CountDownLatch(1);
        CountDownLatch currentRead = new CountDownLatch(1);
        RestartService.Listener listener = new RestartService.Listener() {
            @Override
            public void onCancelled(ShutdownReservation reservation) {
                cancellationEntered.countDown();
                await(releaseCancellation);
            }
        };
        RestartService service = service(scheduler, listener);
        ShutdownReservation automatic = reservation(ReservationSource.AUTOMATIC, Duration.ofHours(1));
        ShutdownReservation manual = reservation(ReservationSource.MANUAL, Duration.ofHours(2));
        service.schedule(automatic);

        Thread replacementThread = daemonThread(() -> service.schedule(manual));
        replacementThread.start();
        Thread readerThread = null;
        try {
            Assertions.assertTrue(cancellationEntered.await(1, TimeUnit.SECONDS));
            readerThread = daemonThread(() -> {
                service.current();
                currentRead.countDown();
            });
            readerThread.start();
            Assertions.assertTrue(currentRead.await(1, TimeUnit.SECONDS));
        } finally {
            releaseCancellation.countDown();
        }

        replacementThread.join(1000);
        Assertions.assertFalse(replacementThread.isAlive());
        if (readerThread != null) {
            readerThread.join(1000);
            Assertions.assertFalse(readerThread.isAlive());
        }
        Assertions.assertEquals(manual, service.current().orElseThrow().reservation());
    }

    @Test
    void testReentrantCancellationReturnsSuperseded() {
        TestScheduler scheduler = new TestScheduler();
        AtomicReference<RestartService> serviceReference = new AtomicReference<>();
        AtomicBoolean cancelOnNotification = new AtomicBoolean();
        RestartService.Listener listener = new RestartService.Listener() {
            @Override
            public void onCancelled(ShutdownReservation reservation) {
                if (cancelOnNotification.get()) {
                    serviceReference.get().cancel();
                }
            }
        };
        RestartService service = service(scheduler, listener);
        serviceReference.set(service);
        ShutdownReservation automatic = reservation(ReservationSource.AUTOMATIC, Duration.ofHours(1));
        ShutdownReservation manual = reservation(ReservationSource.MANUAL, Duration.ofHours(2));
        service.schedule(automatic);

        cancelOnNotification.set(true);
        RestartService.ScheduleResult result = service.schedule(manual);

        Assertions.assertEquals(RestartService.ScheduleStatus.SUPERSEDED, result.status());
        Assertions.assertEquals(automatic, result.replaced());
        Assertions.assertFalse(result.scheduled());
        Assertions.assertTrue(service.current().isEmpty());
        Assertions.assertEquals(4, scheduler.tasks.size());
        Assertions.assertTrue(scheduler.tasks.stream().allMatch(task -> task.cancelled));
    }

    private static RestartService service(TestScheduler scheduler, RestartService.Listener listener) {
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

    private static Thread daemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
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
        private volatile boolean cancelled;

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
