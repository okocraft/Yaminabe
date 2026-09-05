package net.okocraft.yaminabe.velocity.command;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class YaminabeCommands {

    public static DefaultMessageDefiner getDefiner() {
        return CommandMessages.DEFINER;
    }

    public static void register(CommandManager manager, Object plugin, Scheduler scheduler, YaminabeReloader reloader) {
        BrigadierCommand command = createCommand(scheduler, reloader);
        manager.register(manager.metaBuilder(command).plugin(plugin).build(), command);
    }

    static BrigadierCommand createCommand(Scheduler scheduler, YaminabeReloader reloader) {
        return new BrigadierCommand(
            BrigadierCommand.literalArgumentBuilder("yaminabe")
                .requires(source -> source.hasPermission("yaminabe.command"))
                .then(ReloadCommand.createReloadCommand(scheduler, reloader))
                .then(VersionCommand.createVersionCommand())
        );
    }

    private YaminabeCommands() {
        throw new UnsupportedOperationException();
    }
}
