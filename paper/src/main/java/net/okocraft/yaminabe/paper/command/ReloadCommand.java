package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
final class ReloadCommand {

    private static final String PERMISSION = "yaminabe.command.reload";

    static LiteralCommandNode<CommandSourceStack> createReloadCommand(Scheduler async, YaminabeReloader reloader) {
        return Commands.literal("reload")
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .executes(context -> {
                CommandSender sender = context.getSource().getSender();
                sender.sendMessage(CommandMessages.RELOAD_START);
                async.runNow(() -> reloader.reload(notification -> sender.sendMessage(switch (notification) {
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
