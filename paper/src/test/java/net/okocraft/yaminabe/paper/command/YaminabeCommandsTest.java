package net.okocraft.yaminabe.paper.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandMessages;
import net.okocraft.yaminabe.common.restart.command.RestartCommandPermissions;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import net.okocraft.yaminabe.common.restart.execution.RestartExecutionMessages;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

class YaminabeCommandsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-06T03:00:00Z"), ZoneOffset.UTC);
    private static final RestartCommandSettings SETTINGS = new RestartCommandSettings(Duration.ofSeconds(60), ZoneOffset.UTC);

    @Test
    void testAutoRestartCommandUsesCommonCommandTree() throws Exception {
        CommandSender sender = Mockito.mock(CommandSender.class);
        TestSources.grant(sender, RestartCommandPermissions.RESTART);
        CommandSourceStack source = TestSources.ofSenderOnly(sender);
        RestartService service = restartService();
        CommandTester tester = CommandTester.of(YaminabeCommands.createAutoRestartCommand(
            service,
            CLOCK,
            () -> SETTINGS
        ));

        Assertions.assertEquals(1, tester.execute(source, "autorestart restart now"));
        Assertions.assertEquals(ShutdownType.RESTART, service.current().orElseThrow().reservation().type());
    }

    @Test
    void testFoliaRestartCommandUsesImmediateRestartTree() throws Exception {
        CommandSender sender = Mockito.mock(CommandSender.class);
        TestSources.grant(sender, RestartCommandPermissions.RESTART);
        CommandSourceStack source = TestSources.ofSenderOnly(sender);
        RestartService service = restartService();
        CommandTester tester = CommandTester.of(YaminabeCommands.createRestartCommand(
            service,
            CLOCK,
            () -> SETTINGS
        ));

        Assertions.assertEquals(1, tester.execute(source, "restart reason maintenance"));
        var reservation = service.current().orElseThrow().reservation();
        Assertions.assertEquals(ShutdownType.RESTART, reservation.type());
        Assertions.assertEquals("maintenance", reservation.reason());
    }

    @Test
    void testRestartDefinersAreExposedForLanguageLoading() {
        Assertions.assertTrue(YaminabeCommands.getDefiners().containsAll(List.of(
            CommandMessages.DEFINER,
            RestartCommandMessages.DEFINER,
            RestartExecutionMessages.DEFINER
        )));
    }

    private static RestartService restartService() {
        return new RestartService(new TestScheduler(), CLOCK, new RestartService.Listener() {
        });
    }

    private static final class TestScheduler implements Scheduler {

        private static final CancellableTask NOOP_TASK = () -> {
        };

        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            return NOOP_TASK;
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(
            @NotNull Consumer<CancellableTask> task,
            @NotNull Duration interval
        ) {
            return NOOP_TASK;
        }
    }
}
