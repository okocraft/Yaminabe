package net.okocraft.yaminabe.common.restart.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class AutoRestartCommandTest {

    private static final Instant NOW = Instant.parse("2026-09-06T03:00:00Z"); // 12:00 JST
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final RestartCommandSettings SETTINGS = new RestartCommandSettings(Duration.ofSeconds(60), TOKYO);
    private static final RestartCommandSource<TestSource> SOURCE_ADAPTER = new RestartCommandSource<>() {
        @Override
        public boolean hasPermission(TestSource source, String permission) {
            return source.permissions.contains(permission);
        }

        @Override
        public void sendMessage(TestSource source, ComponentLike message) {
            source.messages.add(message);
        }
    };

    @Test
    void testBareRestartUsesDefaultCountdownImmediately() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(1, fixture.dispatcher.execute("autorestart restart", fixture.source));

        ShutdownReservation reservation = current(fixture);
        Assertions.assertEquals(ShutdownType.RESTART, reservation.type());
        Assertions.assertEquals(NOW.plusSeconds(60), reservation.executeAt());
        Assertions.assertEquals(NOW, reservation.countdownStartAt());
        Assertions.assertNull(reservation.reason());
        Assertions.assertEquals(1, fixture.source.messages.size());
    }

    @Test
    void testInReservationStaysSilentUntilDefaultCountdown() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(1, fixture.dispatcher.execute("autorestart restart in 6h", fixture.source));

        ShutdownReservation reservation = current(fixture);
        Assertions.assertEquals(NOW.plus(Duration.ofHours(6)), reservation.executeAt());
        Assertions.assertEquals(reservation.executeAt().minusSeconds(60), reservation.countdownStartAt());
    }

    @Test
    void testCustomCountdownAndReason() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(
            1,
            fixture.dispatcher.execute(
                "autorestart restart in 6h countdown 5m reason maintenance window",
                fixture.source
            )
        );

        ShutdownReservation reservation = current(fixture);
        Assertions.assertEquals(NOW.plus(Duration.ofHours(6)), reservation.executeAt());
        Assertions.assertEquals(reservation.executeAt().minus(Duration.ofMinutes(5)), reservation.countdownStartAt());
        Assertions.assertEquals("maintenance window", reservation.reason());
    }

    @Test
    void testFullCountdownStartsAtReservationTime() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(
            1,
            fixture.dispatcher.execute("autorestart restart in 6h countdown full", fixture.source)
        );

        ShutdownReservation reservation = current(fixture);
        Assertions.assertEquals(NOW, reservation.countdownStartAt());
    }

    @Test
    void testReasonCanStartWithCountdownWord() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(
            1,
            fixture.dispatcher.execute(
                "autorestart restart in 6h reason countdown maintenance",
                fixture.source
            )
        );

        Assertions.assertEquals("countdown maintenance", current(fixture).reason());
    }

    @Test
    void testAbsoluteDateTimeUsesConfiguredTimeZone() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(
            1,
            fixture.dispatcher.execute("autorestart restart at 2026-09-10T18:00", fixture.source)
        );

        Assertions.assertEquals(Instant.parse("2026-09-10T09:00:00Z"), current(fixture).executeAt());
    }

    @Test
    void testTimeOnlyAtAcceptsColon() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(1, fixture.dispatcher.execute("autorestart restart at 18:00", fixture.source));

        Assertions.assertEquals(Instant.parse("2026-09-06T09:00:00Z"), current(fixture).executeAt());
    }

    @Test
    void testAtSupportsCountdownAndReason() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(
            1,
            fixture.dispatcher.execute(
                "autorestart restart at 2026-09-10T18:00 countdown 5m reason maintenance window",
                fixture.source
            )
        );

        ShutdownReservation reservation = current(fixture);
        Assertions.assertEquals(Instant.parse("2026-09-10T09:00:00Z"), reservation.executeAt());
        Assertions.assertEquals(reservation.executeAt().minus(Duration.ofMinutes(5)), reservation.countdownStartAt());
        Assertions.assertEquals("maintenance window", reservation.reason());
    }

    @Test
    void testAtReasonCanStartWithCountdownWord() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(
            1,
            fixture.dispatcher.execute(
                "autorestart restart at 18:00 reason countdown maintenance",
                fixture.source
            )
        );

        Assertions.assertEquals("countdown maintenance", current(fixture).reason());
    }

    @Test
    void testMalformedAtArgumentsDoNotSchedule() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(0, fixture.dispatcher.execute("autorestart restart at 18:00 countdown", fixture.source));

        Assertions.assertTrue(fixture.service.current().isEmpty());
        Assertions.assertTrue(fixture.scheduler.tasks.isEmpty());
        Assertions.assertEquals(1, fixture.source.messages.size());
    }

    @Test
    void testStopCreatesStopReservation() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.STOP);

        Assertions.assertEquals(1, fixture.dispatcher.execute("autorestart stop in 10m", fixture.source));

        Assertions.assertEquals(ShutdownType.STOP, current(fixture).type());
    }

    @Test
    void testCancelCancelsCurrentReservation() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);
        fixture.source.permissions.add(RestartCommandPermissions.CANCEL);
        fixture.dispatcher.execute("autorestart restart in 10m", fixture.source);

        Assertions.assertEquals(1, fixture.dispatcher.execute("autorestart cancel", fixture.source));

        Assertions.assertTrue(fixture.service.current().isEmpty());
        Assertions.assertTrue(fixture.scheduler.tasks.stream().allMatch(task -> task.cancelled));
    }

    @Test
    void testCancelWithoutReservationReturnsZero() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.CANCEL);

        Assertions.assertEquals(0, fixture.dispatcher.execute("autorestart cancel", fixture.source));
        Assertions.assertEquals(1, fixture.source.messages.size());
    }

    @Test
    void testInvalidDurationDoesNotSchedule() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(0, fixture.dispatcher.execute("autorestart restart in tomorrow", fixture.source));

        Assertions.assertTrue(fixture.service.current().isEmpty());
        Assertions.assertTrue(fixture.scheduler.tasks.isEmpty());
        Assertions.assertEquals(1, fixture.source.messages.size());
    }

    @Test
    void testInvalidDateTimeDoesNotSchedule() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(0, fixture.dispatcher.execute("autorestart restart at someday", fixture.source));

        Assertions.assertTrue(fixture.service.current().isEmpty());
        Assertions.assertTrue(fixture.scheduler.tasks.isEmpty());
        Assertions.assertEquals(1, fixture.source.messages.size());
    }

    @Test
    void testRestartSubcommandIsHiddenWithoutPermission() {
        Fixture fixture = fixture();

        Assertions.assertThrows(
            CommandSyntaxException.class,
            () -> fixture.dispatcher.execute("autorestart restart", fixture.source)
        );
        Assertions.assertTrue(fixture.service.current().isEmpty());
    }

    private static Fixture fixture() {
        TestScheduler scheduler = new TestScheduler();
        RestartService service = new RestartService(
            scheduler,
            Clock.fixed(NOW, ZoneOffset.UTC),
            new RestartService.Listener() {
            }
        );
        CommandDispatcher<TestSource> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(AutoRestartCommand.create(
            "autorestart",
            service,
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> SETTINGS,
            SOURCE_ADAPTER
        ));
        return new Fixture(dispatcher, service, scheduler, new TestSource());
    }

    private static ShutdownReservation current(Fixture fixture) {
        return fixture.service.current().orElseThrow().reservation();
    }

    private record Fixture(
        CommandDispatcher<TestSource> dispatcher,
        RestartService service,
        TestScheduler scheduler,
        TestSource source
    ) {
    }

    private static final class TestSource {
        private final Set<String> permissions = new HashSet<>();
        private final List<ComponentLike> messages = new ArrayList<>();
    }

    private static final class TestScheduler implements Scheduler {
        private final List<TestTask> tasks = new ArrayList<>();

        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            TestTask scheduled = new TestTask(task, delay);
            this.tasks.add(scheduled);
            return scheduled;
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
        @SuppressWarnings("unused")
        private final Runnable runnable;
        @SuppressWarnings("unused")
        private final Duration delay;
        private boolean cancelled;

        private TestTask(Runnable runnable, Duration delay) {
            this.runnable = runnable;
            this.delay = delay;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }
    }
}
