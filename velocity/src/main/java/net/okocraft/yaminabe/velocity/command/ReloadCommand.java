package net.okocraft.yaminabe.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
final class ReloadCommand {

    private static final String PERMISSION = "yaminabe.command.reload";

    static LiteralCommandNode<CommandSource> createReloadCommand(Scheduler async, YaminabeReloader reloader) {
        return BrigadierCommand.literalArgumentBuilder("reload")
            .requires(source -> source.hasPermission(PERMISSION))
            .executes(context -> {
                CommandSource source = context.getSource();
                source.sendMessage(CommandMessages.RELOAD_START);
                async.runNow(() -> reloader.reload(notification -> source.sendMessage(switch (notification) {
                    case CONFIG_RELOADED -> CommandMessages.RELOAD_CONFIG_RELOADED;
                    case FAILED_TO_RELOAD_CONFIG -> CommandMessages.RELOAD_CONFIG_FAILED;
                    case LANGUAGE_RELOADED -> CommandMessages.RELOAD_LANGUAGE_RELOADED;
                    case FAILED_TO_RELOAD_LANGUAGES -> CommandMessages.RELOAD_LANGUAGE_FAILED;
                })));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    private ReloadCommand() {
        throw new UnsupportedOperationException();
    }
}
