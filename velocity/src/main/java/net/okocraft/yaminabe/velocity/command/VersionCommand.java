package net.okocraft.yaminabe.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
final class VersionCommand {

    static final String UNKNOWN_VERSION = "unknown";

    static LiteralCommandNode<CommandSource> createVersionCommand() {
        return BrigadierCommand.literalArgumentBuilder("version")
            .requires(source -> source.hasPermission("yaminabe.command.version"))
            .executes(context -> {
                context.getSource().sendMessage(CommandMessages.VERSION_PRINT.apply(detectVersion()));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    private static String detectVersion() {
        String version = VersionCommand.class.getPackage().getImplementationVersion();
        return version != null ? version : UNKNOWN_VERSION;
    }

    private VersionCommand() {
        throw new UnsupportedOperationException();
    }
}
