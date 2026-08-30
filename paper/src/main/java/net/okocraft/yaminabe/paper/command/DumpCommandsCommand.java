package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

@NotNullByDefault
final class DumpCommandsCommand {

    private static final String PERMISSION = "yaminabe.command.dumpcommands";

    static LiteralCommandNode<CommandSourceStack> createDumpCommandsCommand(Supplier<? extends Collection<String>> commandLabels) {
        Objects.requireNonNull(commandLabels);

        return Commands.literal("dumpcommands")
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .executes(context -> {
                var labels = new ArrayList<>(commandLabels.get());
                labels.sort(String::compareTo);

                String list = labels.isEmpty() ? "(none)" : "- " + String.join("\n- ", labels);
                context.getSource().getSender().sendMessage(Component.text("Registered commands (" + labels.size() + "):\n" + list));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    private DumpCommandsCommand() {
        throw new UnsupportedOperationException();
    }
}
