package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
final class DisposalCommand {

    private static final String COMMAND_NAME = "disposal";
    private static final String PERMISSION = "yaminabe.command.disposal";

    static List<String> getAliases() {
        return List.of("trash");
    }

    static LiteralCommandNode<CommandSourceStack> createDisposalCommand() {
        return createDisposalCommand(DisposalCommand::createDisposalMenu);
    }

    static LiteralCommandNode<CommandSourceStack> createDisposalCommand(MenuFactory menuFactory) {
        return MenuCommand.create(
            COMMAND_NAME,
            PERMISSION,
            CommandMessages.DISPOSAL_OPENING,
            CommandMessages.DISPOSAL_PLAYER_ONLY,
            player -> menuFactory.createMenu(player, CommandMessages.DISPOSAL_TITLE.asComponent())
        );
    }

    private static InventoryView createDisposalMenu(Player player, Component title) {
        return MenuType.GENERIC_9X4.create(player, title);
    }

    @FunctionalInterface
    interface MenuFactory {
        InventoryView createMenu(Player player, Component title);
    }
}
