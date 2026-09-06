package net.okocraft.yaminabe.velocity.command;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandMessages;
import net.okocraft.yaminabe.common.restart.command.RestartCommandPermissions;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownMessages;
import net.okocraft.yaminabe.common.restart.execution.RestartExecutionMessages;
import net.okocraft.yaminabe.velocity.testsupport.CommandTester;
import net.okocraft.yaminabe.velocity.testsupport.TestSources;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class YaminabeCommandsTest {

    private static final YaminabeReloader NOOP_RELOADER = consumer -> {
    };
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-06T03:00:00Z"), ZoneOffset.UTC);
    private static final RestartCommandSettings RESTART_SETTINGS = new RestartCommandSettings(
        Duration.ofSeconds(60),
        ZoneId.of("Asia/Tokyo")
    );

    @Test
    void testVersionCommandIsWiredUnderYaminabeRoot() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, "yaminabe.command", "yaminabe.command.version");
        RecordingScheduler scheduler = new RecordingScheduler();

        CommandTester tester = CommandTester.of(YaminabeCommands.createCommand(scheduler, NOOP_RELOADER));

        Assertions.assertEquals(1, tester.execute(console, "yaminabe version"));
        Mockito.verify(console).sendMessage(CommandMessages.VERSION_PRINT.apply(VersionCommand.UNKNOWN_VERSION));
        Assertions.assertEquals(0, scheduler.runNowCalls);
        Assertions.assertTrue(scheduler.delayed.isEmpty());
        Assertions.assertTrue(scheduler.repeating.isEmpty());
    }

    @Test
    void testReloadCommandIsWiredUnderYaminabeRoot() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, "yaminabe.command", "yaminabe.command.reload");
        RecordingScheduler scheduler = new RecordingScheduler();

        YaminabeReloader reloader = consumer -> consumer.accept(YaminabeReloader.Notification.CONFIG_RELOADED);
        CommandTester tester = CommandTester.of(YaminabeCommands.createCommand(scheduler, reloader));

        Assertions.assertEquals(1, tester.execute(console, "yaminabe reload"));

        InOrder order = Mockito.inOrder(console);
        order.verify(console).sendMessage(CommandMessages.RELOAD_START);
        order.verify(console).sendMessage(CommandMessages.RELOAD_CONFIG_RELOADED);
        order.verifyNoMoreInteractions();
        Assertions.assertEquals(1, scheduler.runNowCalls);
        Assertions.assertTrue(scheduler.delayed.isEmpty());
        Assertions.assertTrue(scheduler.repeating.isEmpty());
    }

    @Test
    void testAutoRestartCommandUsesCommonCommandTree() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, RestartCommandPermissions.RESTART);
        RecordingScheduler scheduler = new RecordingScheduler();
        RestartService service = restartService(scheduler);
        CommandTester tester = CommandTester.of(YaminabeCommands.createAutoRestartCommand(
            service,
            CLOCK,
            () -> RESTART_SETTINGS
        ));

        Assertions.assertEquals(1, tester.execute(console, "autorestart restart now"));
        Assertions.assertEquals(ShutdownType.RESTART, service.current().orElseThrow().reservation().type());
        Assertions.assertEquals(List.of(Duration.ZERO, Duration.ZERO), scheduler.delayed);
        Assertions.assertTrue(scheduler.repeating.isEmpty());
    }

    @Test
    void testVelocityRestartCommandUsesImmediateRestartTree() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, RestartCommandPermissions.RESTART);
        RecordingScheduler scheduler = new RecordingScheduler();
        RestartService service = restartService(scheduler);
        CommandTester tester = CommandTester.of(YaminabeCommands.createVelocityRestartCommand(
            service,
            CLOCK,
            () -> RESTART_SETTINGS
        ));

        Assertions.assertEquals(1, tester.execute(console, "vrestart reason maintenance"));
        var reservation = service.current().orElseThrow().reservation();
        Assertions.assertEquals(ShutdownType.RESTART, reservation.type());
        Assertions.assertEquals("maintenance", reservation.reason());
        Assertions.assertEquals(List.of(Duration.ZERO, Duration.ZERO), scheduler.delayed);
        Assertions.assertTrue(scheduler.repeating.isEmpty());
    }

    @Test
    void testRestartDefinersAreExposedForLanguageLoading() {
        Assertions.assertTrue(YaminabeCommands.getDefiners().containsAll(List.of(
            CommandMessages.DEFINER,
            RestartCommandMessages.DEFINER,
            RestartExecutionMessages.DEFINER,
            RestartCountdownMessages.DEFINER
        )));
    }

    private static RestartService restartService(Scheduler scheduler) {
        return new RestartService(scheduler, CLOCK, new RestartService.Listener() {
        });
    }

    private static final class RecordingScheduler implements Scheduler {

        private static final CancellableTask NOOP_TASK = () -> {
        };
        private int runNowCalls;
        private final List<Duration> delayed = new ArrayList<>();
        private final List<Duration> repeating = new ArrayList<>();

        @Override
        public void runNow(@NotNull Runnable task) {
            this.runNowCalls++;
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            this.delayed.add(delay);
            return NOOP_TASK;
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval) {
            this.repeating.add(interval);
            return NOOP_TASK;
        }
    }
}
