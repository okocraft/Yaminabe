package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
final class DumpCommandsCommand {

    private static final String PERMISSION = "yaminabe.command.dumpcommands";

    static LiteralCommandNode<CommandSourceStack> createDumpCommandsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("dumpcommands")
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .executes(context -> {
                var labels = dispatcher.getRoot().getChildren().stream()
                    .map(node -> node.getName())
                    .sorted()
                    .toList();

                var message = Component.text()
                    .append(Component.text("Registered commands (" + labels.size() + "):"))
                    .append(Component.newline());

                if (labels.isEmpty()) {
                    message.append(Component.text("(none)"));
                } else {
                    for (int i = 0; i < labels.size(); i++) {
                        if (i > 0) {
                            message.append(Component.newline());
                        }
                        message.append(Component.text("- " + labels.get(i)));
                    }
                }

                context.getSource().getSender().sendMessage(message.build());
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    private DumpCommandsCommand() {
        throw new UnsupportedOperationException();
    }
}
