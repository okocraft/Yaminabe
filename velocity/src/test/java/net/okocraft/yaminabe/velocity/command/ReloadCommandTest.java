package net.okocraft.yaminabe.velocity.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import net.kyori.adventure.text.Component;
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

class ReloadCommandTest {

    private static final String PERMISSION = "yaminabe.command.reload";

    @Test
    void testNotificationsOfSuccessAreSent() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, PERMISSION);

        var reloader = new NotifyingReloader(YaminabeReloader.Notification.CONFIG_RELOADED, YaminabeReloader.Notification.LANGUAGE_RELOADED);

        Assertions.assertEquals(1, tester(reloader).execute(console, "reload"));

        Assertions.assertEquals(1, reloader.count);

        InOrder order = Mockito.inOrder(console);
        order.verify(console).sendMessage(CommandMessages.RELOAD_START);
        order.verify(console).sendMessage(CommandMessages.RELOAD_CONFIG_RELOADED);
        order.verify(console).sendMessage(CommandMessages.RELOAD_LANGUAGE_RELOADED);
        order.verifyNoMoreInteractions();
    }

    @Test
    void testNotificationsOfFailureAreSent() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, PERMISSION);

        var reloader = new NotifyingReloader(YaminabeReloader.Notification.FAILED_TO_RELOAD_CONFIG, YaminabeReloader.Notification.FAILED_TO_RELOAD_LANGUAGES);

        Assertions.assertEquals(1, tester(reloader).execute(console, "reload"));

        Assertions.assertEquals(1, reloader.count);

        InOrder order = Mockito.inOrder(console);
        order.verify(console).sendMessage(CommandMessages.RELOAD_START);
        order.verify(console).sendMessage(CommandMessages.RELOAD_CONFIG_FAILED);
        order.verify(console).sendMessage(CommandMessages.RELOAD_LANGUAGE_FAILED);
        order.verifyNoMoreInteractions();
    }

    @Test
    void testCommandIsHiddenWithUnsetPermission() {
        this.assertHidden(TestSources.console());
    }

    @Test
    void testCommandIsHiddenWithDeniedPermission() {
        ConsoleCommandSource console = TestSources.console();
        TestSources.deny(console, PERMISSION);

        this.assertHidden(console);
    }

    @Test
    void testCommandIsHiddenWithAnotherPermission() {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, "yaminabe.command");

        this.assertHidden(console);
    }

    private void assertHidden(CommandSource source) {
        var reloader = new NotifyingReloader(YaminabeReloader.Notification.CONFIG_RELOADED, YaminabeReloader.Notification.LANGUAGE_RELOADED);

        Assertions.assertThrows(CommandSyntaxException.class, () -> tester(reloader).execute(source, "reload"));

        Assertions.assertEquals(0, reloader.count);
        Mockito.verify(source, Mockito.never()).sendMessage(Mockito.any(Component.class));
    }

    private static CommandTester tester(YaminabeReloader reloader) {
        return CommandTester.of(ReloadCommand.createReloadCommand(new ImmediateScheduler(), reloader));
    }

    private static final class NotifyingReloader implements YaminabeReloader {

        private final Notification[] notifications;
        private int count;

        private NotifyingReloader(Notification... notifications) {
            this.notifications = notifications;
        }

        @Override
        public void reload(Consumer<Notification> consumer) {
            this.count++;
            for (Notification notification : this.notifications) {
                consumer.accept(notification);
            }
        }
    }

    private static final class ImmediateScheduler implements Scheduler {

        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval) {
            throw new UnsupportedOperationException();
        }
    }
}
