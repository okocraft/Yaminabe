package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.siroshun.mcmsgdef.MessageKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
enum WorkstationCommands {

    ANVIL("anvil", CommandMessages.ANVIL_PLAYER_ONLY, MenuType.ANVIL::create),
    CARTOGRAPHY_TABLE("cartographytable", CommandMessages.CARTOGRAPHYTABLE_PLAYER_ONLY, MenuType.CARTOGRAPHY_TABLE::create),
    GRINDSTONE("grindstone", CommandMessages.GRINDSTONE_PLAYER_ONLY, MenuType.GRINDSTONE::create),
    LOOM("loom", CommandMessages.LOOM_PLAYER_ONLY, MenuType.LOOM::create),
    SMITHING_TABLE("smithingtable", CommandMessages.SMITHINGTABLE_PLAYER_ONLY, MenuType.SMITHING::create),
    STONECUTTER("stonecutter", CommandMessages.STONECUTTER_PLAYER_ONLY, MenuType.STONECUTTER::create),
    WORKBENCH("workbench", CommandMessages.WORKBENCH_PLAYER_ONLY, MenuType.CRAFTING::create, "craft");

    private final String commandName;
    private final String permission;
    private final MessageKey.Arg1<String> playerOnlyMessage;
    private final MenuCommand.MenuFactory menuFactory;
    private final List<String> aliases;

    WorkstationCommands(String commandName, MessageKey.Arg1<String> playerOnlyMessage, MenuCommand.MenuFactory menuFactory, String... aliases) {
        this.commandName = commandName;
        this.permission = "yaminabe.command." + commandName;
        this.playerOnlyMessage = playerOnlyMessage;
        this.menuFactory = menuFactory;
        this.aliases = List.of(aliases);
    }

    String getCommandName() {
        return this.commandName;
    }

    String getPermission() {
        return this.permission;
    }

    MessageKey.Arg1<String> getPlayerOnlyMessage() {
        return this.playerOnlyMessage;
    }

    List<String> getAliases() {
        return this.aliases;
    }

    LiteralCommandNode<CommandSourceStack> createCommand() {
        return this.createCommand(this.menuFactory);
    }

    LiteralCommandNode<CommandSourceStack> createCommand(MenuCommand.MenuFactory menuFactory) {
        return MenuCommand.create(this.commandName, this.permission, null, this.playerOnlyMessage, menuFactory);
    }
}
