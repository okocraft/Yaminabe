package net.okocraft.yaminabe.velocity.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.velocity.testsupport.CommandTester;
import net.okocraft.yaminabe.velocity.testsupport.TestSources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class VersionCommandTest {

    private static final String PERMISSION = "yaminabe.command.version";

    private final CommandTester tester = CommandTester.of(VersionCommand.createVersionCommand());

    @Test
    void testVersionIsPrintedToConsole() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(1, this.tester.execute(console, "version"));

        Mockito.verify(console).sendMessage(CommandMessages.VERSION_PRINT.apply(VersionCommand.UNKNOWN_VERSION));
    }

    @Test
    void testVersionIsPrintedToPlayer() throws Exception {
        Player player = TestSources.player();
        TestSources.grant(player, PERMISSION);

        Assertions.assertEquals(1, this.tester.execute(player, "version"));

        Mockito.verify(player).sendMessage(CommandMessages.VERSION_PRINT.apply(VersionCommand.UNKNOWN_VERSION));
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
        Assertions.assertThrows(CommandSyntaxException.class, () -> this.tester.execute(source, "version"));

        Mockito.verify(source, Mockito.never()).sendMessage(Mockito.any(Component.class));
    }
}
