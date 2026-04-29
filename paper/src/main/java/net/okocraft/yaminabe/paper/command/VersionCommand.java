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
                context.getSource().getSender().sendPlainMessage("Yaminabe Version: " + VersionCommand.class.getPackage().getImplementationVersion());
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }
}
