package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class DumpCommandsCommandTest {

    private static final String PERMISSION = "yaminabe.command.dumpcommands";

    @Test
    void testCommandsAreSortedAndPrinted() throws Exception {
        var tester = CommandTester.of(DumpCommandsCommand.createDumpCommandsCommand(
            () -> List.of("zeta", "minecraft:help", "alpha")
        ));
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(1, tester.execute(TestSources.ofSenderOnly(console), "dumpcommands"));
        Mockito.verify(console).sendMessage(Component.text(
            "Registered commands (3):\n- alpha\n- minecraft:help\n- zeta"
        ));
    }

    @Test
    void testEmptyCommandListIsPrinted() throws Exception {
        var tester = CommandTester.of(DumpCommandsCommand.createDumpCommandsCommand(List::of));
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(1, tester.execute(TestSources.ofSenderOnly(console), "dumpcommands"));
        Mockito.verify(console).sendMessage(Component.text("Registered commands (0):\n(none)"));
    }

    @Test
    void testCommandIsHiddenWithoutPermission() {
        var tester = CommandTester.of(DumpCommandsCommand.createDumpCommandsCommand(List::of));
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);

        Assertions.assertThrows(
            CommandSyntaxException.class,
            () -> tester.execute(TestSources.ofSenderOnly(console), "dumpcommands")
        );
        Mockito.verify(console, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}
