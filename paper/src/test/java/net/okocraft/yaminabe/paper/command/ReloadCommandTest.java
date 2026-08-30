package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.ConsoleCommandSender;
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
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        var reloader = new NotifyingReloader(YaminabeReloader.Notification.CONFIG_RELOADED, YaminabeReloader.Notification.LANGUAGE_RELOADED);

        Assertions.assertEquals(1, tester(reloader).execute(TestSources.ofSenderOnly(console), "reload"));

        Assertions.assertEquals(1, reloader.count);

        InOrder order = Mockito.inOrder(console);
        order.verify(console).sendMessage(CommandMessages.RELOAD_START);
        order.verify(console).sendMessage(CommandMessages.RELOAD_CONFIG_RELOADED);
        order.verify(console).sendMessage(CommandMessages.RELOAD_LANGUAGE_RELOADED);
        order.verifyNoMoreInteractions();
    }

    @Test
    void testNotificationsOfFailureAreSent() throws Exception {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        var reloader = new NotifyingReloader(YaminabeReloader.Notification.FAILED_TO_RELOAD_CONFIG, YaminabeReloader.Notification.FAILED_TO_RELOAD_LANGUAGES);

        Assertions.assertEquals(1, tester(reloader).execute(TestSources.ofSenderOnly(console), "reload"));

        Assertions.assertEquals(1, reloader.count);

        InOrder order = Mockito.inOrder(console);
        order.verify(console).sendMessage(CommandMessages.RELOAD_START);
        order.verify(console).sendMessage(CommandMessages.RELOAD_CONFIG_FAILED);
        order.verify(console).sendMessage(CommandMessages.RELOAD_LANGUAGE_FAILED);
        order.verifyNoMoreInteractions();
    }

    @Test
    void testCommandIsHiddenWithUnsetPermission() {
        this.assertHidden(Mockito.mock(ConsoleCommandSender.class));
    }

    @Test
    void testCommandIsHiddenWithDeniedPermission() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.deny(console, PERMISSION);

        this.assertHidden(console);
    }

    @Test
    void testCommandIsHiddenWithAnotherPermission() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, "yaminabe.command");

        this.assertHidden(console);
    }

    private void assertHidden(ConsoleCommandSender console) {
        var reloader = new NotifyingReloader(YaminabeReloader.Notification.CONFIG_RELOADED, YaminabeReloader.Notification.LANGUAGE_RELOADED);

        Assertions.assertThrows(CommandSyntaxException.class, () -> tester(reloader).execute(TestSources.ofSenderOnly(console), "reload"));

        Assertions.assertEquals(0, reloader.count);
        Mockito.verify(console, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
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
        public void runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval) {
            throw new UnsupportedOperationException();
        }
    }
}
