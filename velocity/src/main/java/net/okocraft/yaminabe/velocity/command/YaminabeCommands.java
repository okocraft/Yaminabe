package net.okocraft.yaminabe.velocity.command;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class YaminabeCommands {

    public static DefaultMessageDefiner getDefiner() {
        return CommandMessages.DEFINER;
    }

    public static void register(CommandManager manager, Object plugin) {
        BrigadierCommand command = createCommand();
        manager.register(manager.metaBuilder(command).plugin(plugin).build(), command);
    }

    static BrigadierCommand createCommand() {
        return new BrigadierCommand(
            BrigadierCommand.literalArgumentBuilder("yaminabe")
                .requires(source -> source.hasPermission("yaminabe.command"))
                .then(VersionCommand.createVersionCommand())
        );
    }

    private YaminabeCommands() {
        throw new UnsupportedOperationException();
    }
}
