package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
final class VersionCommand {

    static LiteralCommandNode<CommandSourceStack> createVersionCommand() {
        return Commands.literal("version")
            .requires(source -> source.getSender().hasPermission("yaminabe.command.version"))
            .executes(context -> {
                String version = VersionCommand.class.getPackage().getImplementationVersion();
                context.getSource().getSender().sendMessage(CommandMessages.VERSION_PRINT.apply(version));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }
}
