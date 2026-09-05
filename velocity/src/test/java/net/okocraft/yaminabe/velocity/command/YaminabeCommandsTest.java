package net.okocraft.yaminabe.velocity.command;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import net.okocraft.yaminabe.velocity.testsupport.CommandTester;
import net.okocraft.yaminabe.velocity.testsupport.TestSources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class YaminabeCommandsTest {

    @Test
    void testVersionCommandIsWiredUnderYaminabeRoot() throws Exception {
        ConsoleCommandSource console = TestSources.console();
        TestSources.grant(console, "yaminabe.command", "yaminabe.command.version");

        CommandTester tester = CommandTester.of(YaminabeCommands.createCommand());

        Assertions.assertEquals(1, tester.execute(console, "yaminabe version"));
        Mockito.verify(console).sendMessage(CommandMessages.VERSION_PRINT.apply(VersionCommand.UNKNOWN_VERSION));
    }
}
