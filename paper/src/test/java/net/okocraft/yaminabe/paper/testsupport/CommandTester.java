package net.okocraft.yaminabe.paper.testsupport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;

/**
 * Runs a command through a {@link CommandDispatcher}, the way the server runs it, so that what a test goes through is
 * the command tree itself, including the permission its nodes require and the arguments they take.
 */
public final class CommandTester {

    /**
     * Creates a tester for the given command.
     */
    public static CommandTester of(LiteralCommandNode<CommandSourceStack> command) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(command);
        return new CommandTester(dispatcher);
    }

    private final CommandDispatcher<CommandSourceStack> dispatcher;

    private CommandTester(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Runs the given input and returns what the command returned, which is {@code 0} for a command that did nothing.
     */
    public int execute(CommandSourceStack source, String input) throws CommandSyntaxException {
        return this.dispatcher.execute(input, source);
    }

    /**
     * Returns what the given input is suggested to be completed with.
     */
    public List<String> suggest(CommandSourceStack source, String input) {
        return this.dispatcher.getCompletionSuggestions(this.dispatcher.parse(input, source))
            .join()
            .getList()
            .stream()
            .map(Suggestion::getText)
            .toList();
    }
}
