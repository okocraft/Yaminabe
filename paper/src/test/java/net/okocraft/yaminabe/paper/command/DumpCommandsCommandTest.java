package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DumpCommandsCommandTest {

    private static final String PERMISSION = "yaminabe.command.dumpcommands";

    @Test
    void testCommandsAreReadFromDispatcherWhenExecuted() throws Exception {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var tester = CommandTester.of(DumpCommandsCommand.createDumpCommandsCommand(dispatcher));

        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("zeta"));
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("minecraft:help"));
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("alpha"));

        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(1, tester.execute(TestSources.ofSenderOnly(console), "dumpcommands"));
        Mockito.verify(console).sendMessage(
            Component.text()
                .append(Component.text("Registered commands (3):"))
                .append(Component.newline())
                .append(Component.text("- alpha"))
                .append(Component.newline())
                .append(Component.text("- minecraft:help"))
                .append(Component.newline())
                .append(Component.text("- zeta"))
                .build()
        );
    }

    @Test
    void testEmptyCommandListIsPrinted() throws Exception {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var tester = CommandTester.of(DumpCommandsCommand.createDumpCommandsCommand(dispatcher));
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        TestSources.grant(console, PERMISSION);

        Assertions.assertEquals(1, tester.execute(TestSources.ofSenderOnly(console), "dumpcommands"));
        Mockito.verify(console).sendMessage(
            Component.text()
                .append(Component.text("Registered commands (0):"))
                .append(Component.newline())
                .append(Component.text("(none)"))
                .build()
        );
    }

    @Test
    void testCommandIsHiddenWithoutPermission() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        var tester = CommandTester.of(DumpCommandsCommand.createDumpCommandsCommand(dispatcher));
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);

        Assertions.assertThrows(
            CommandSyntaxException.class,
            () -> tester.execute(TestSources.ofSenderOnly(console), "dumpcommands")
        );
        Mockito.verify(console, Mockito.never()).sendMessage(Mockito.any(ComponentLike.class));
    }
}
