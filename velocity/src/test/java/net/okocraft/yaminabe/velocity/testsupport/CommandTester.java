package net.okocraft.yaminabe.velocity.testsupport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;

import java.util.List;

/**
 * Runs a command through a {@link CommandDispatcher}, the way the proxy runs it, so that what a test goes through is
 * the command tree itself, including the permission its nodes require and the arguments they take.
 */
public final class CommandTester {

    public static CommandTester of(BrigadierCommand command) {
        return of(command.getNode());
    }

    public static CommandTester of(LiteralCommandNode<CommandSource> command) {
        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(command);
        return new CommandTester(dispatcher);
    }

    private final CommandDispatcher<CommandSource> dispatcher;

    private CommandTester(CommandDispatcher<CommandSource> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public int execute(CommandSource source, String input) throws CommandSyntaxException {
        return this.dispatcher.execute(input, source);
    }

    public List<String> suggest(CommandSource source, String input) {
        return this.dispatcher.getCompletionSuggestions(this.dispatcher.parse(input, source))
            .join()
            .getList()
            .stream()
            .map(Suggestion::getText)
            .toList();
    }
}
