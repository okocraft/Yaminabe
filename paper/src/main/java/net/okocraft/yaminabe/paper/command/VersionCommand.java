package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
final class VersionCommand {

    static final String UNKNOWN_VERSION = "unknown";

    static LiteralCommandNode<CommandSourceStack> createVersionCommand() {
        return Commands.literal("version")
            .requires(source -> source.getSender().hasPermission("yaminabe.command.version"))
            .executes(context -> {
                context.getSource().getSender().sendMessage(CommandMessages.VERSION_PRINT.apply(detectVersion()));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    private static String detectVersion() {
        String version = VersionCommand.class.getPackage().getImplementationVersion();
        return version != null ? version : UNKNOWN_VERSION;
    }
}
