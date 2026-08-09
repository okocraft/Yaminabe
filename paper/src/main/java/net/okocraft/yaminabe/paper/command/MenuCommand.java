package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.siroshun.mcmsgdef.MessageKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a command that takes no argument and opens a menu for the player who ran it.
 */
@NotNullByDefault
final class MenuCommand {

    static LiteralCommandNode<CommandSourceStack> create(String commandName,
                                                         String permission,
                                                         @Nullable MessageKey openingMessage,
                                                         MessageKey.Arg1<String> playerOnlyMessage,
                                                         MenuFactory menuFactory) {
        return Commands.literal(commandName)
            .requires(source -> source.getSender().hasPermission(permission))
            .executes(context -> {
                if (!(context.getSource().getExecutor() instanceof Player player)) {
                    context.getSource().getSender().sendMessage(playerOnlyMessage.apply("/" + commandName));
                    return 0;
                }

                if (openingMessage != null) {
                    player.sendMessage(openingMessage);
                }

                player.openInventory(menuFactory.createMenu(player));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    @FunctionalInterface
    interface MenuFactory {
        InventoryView createMenu(Player player);
    }

    private MenuCommand() {
        throw new UnsupportedOperationException();
    }
}
