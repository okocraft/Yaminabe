package net.okocraft.yaminabe.paper.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import io.papermc.paper.command.brigadier.Commands;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class YaminabeCommands {

    public static DefaultMessageDefiner getDefiner() {
        return CommandMessages.DEFINER;
    }

    public static void register(Commands commands) {
        commands.register(
            Commands.literal("yaminabe")
                .requires(source -> source.getSender().hasPermission("yaminabe.command"))
                .then(VersionCommand.createVersionCommand())
                .build()
        );
    }

}
