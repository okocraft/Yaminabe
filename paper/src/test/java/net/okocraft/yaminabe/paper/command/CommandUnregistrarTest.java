package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;

class CommandUnregistrarTest {

    @Test
    void testUnregisterRemovesOnlyRequestedLabel() {
        var commandMap = Mockito.mock(CommandMap.class);
        var knownCommands = new HashMap<String, Command>();
        Mockito.when(commandMap.getKnownCommands()).thenReturn(knownCommands);

        var externalCommand = Mockito.mock(Command.class);
        knownCommands.put("hat", externalCommand);
        knownCommands.put("essentials:hat", externalCommand);

        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        dispatcher.register(LiteralArgumentBuilder.literal("hat"));
        dispatcher.register(LiteralArgumentBuilder.literal("essentials:hat"));

        var commands = Mockito.mock(Commands.class);
        Mockito.when(commands.getDispatcher()).thenReturn(dispatcher);

        CommandUnregistrar.unregister(commands, commandMap, List.of("HAT"));

        Assertions.assertFalse(knownCommands.containsKey("hat"));
        Assertions.assertSame(externalCommand, knownCommands.get("essentials:hat"));
        Assertions.assertNull(dispatcher.getRoot().getChild("hat"));
        Assertions.assertNotNull(dispatcher.getRoot().getChild("essentials:hat"));
    }

    @Test
    void testUnregisterNormalizesLeadingSlashAndWhitespace() {
        var commandMap = Mockito.mock(CommandMap.class);
        var knownCommands = new HashMap<String, Command>();
        Mockito.when(commandMap.getKnownCommands()).thenReturn(knownCommands);
        knownCommands.put("custom", Mockito.mock(Command.class));

        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        dispatcher.register(LiteralArgumentBuilder.literal("custom"));

        var commands = Mockito.mock(Commands.class);
        Mockito.when(commands.getDispatcher()).thenReturn(dispatcher);

        CommandUnregistrar.unregister(commands, commandMap, List.of("  /CuStOm  ", "   "));

        Assertions.assertFalse(knownCommands.containsKey("custom"));
        Assertions.assertNull(dispatcher.getRoot().getChild("custom"));
    }
}
