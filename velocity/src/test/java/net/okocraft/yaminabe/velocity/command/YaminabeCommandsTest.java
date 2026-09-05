package net.okocraft.yaminabe.velocity.command;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.velocity.testsupport.CommandTester;
import net.okocraft.yaminabe.velocity.testsupport.TestSources;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.function.Consumer;

class YaminabeCommandsTest {

    private static final Scheduler IMMEDIATE_SCHEDULER = new ImmediateScheduler();
    private static final YaminabeReloader NOOP_RELOADER = consumer -> {
    };

    @Test
    void testVersionCommandIsWiredUnderYaminabeRoot() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, "yaminabe.command", "yaminabe.command.version");

        CommandTester tester = CommandTester.of(YaminabeCommands.createCommand(IMMEDIATE_SCHEDULER, NOOP_RELOADER));

        Assertions.assertEquals(1, tester.execute(console, "yaminabe version"));
        Mockito.verify(console).sendMessage(CommandMessages.VERSION_PRINT.apply(VersionCommand.UNKNOWN_VERSION));
    }

    @Test
    void testReloadCommandIsWiredUnderYaminabeRoot() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, "yaminabe.command", "yaminabe.command.reload");

        YaminabeReloader reloader = consumer -> consumer.accept(YaminabeReloader.Notification.CONFIG_RELOADED);
        CommandTester tester = CommandTester.of(YaminabeCommands.createCommand(IMMEDIATE_SCHEDULER, reloader));

        Assertions.assertEquals(1, tester.execute(console, "yaminabe reload"));

        InOrder order = Mockito.inOrder(console);
        order.verify(console).sendMessage(CommandMessages.RELOAD_START);
        order.verify(console).sendMessage(CommandMessages.RELOAD_CONFIG_RELOADED);
        order.verifyNoMoreInteractions();
    }

    private static final class ImmediateScheduler implements Scheduler {

        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public void runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval) {
            throw new UnsupportedOperationException();
        }
    }
}
