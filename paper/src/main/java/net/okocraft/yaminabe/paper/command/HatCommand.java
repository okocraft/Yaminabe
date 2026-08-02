package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

@NotNullByDefault
final class HatCommand {

    private static final String COMMAND_NAME = "hat";
    private static final String PERMISSION = "yaminabe.command.hat";
    private static final String ALLOW_TYPE_PERMISSION_PREFIX = PERMISSION + ".allow-type.";
    private static final String IGNORE_BINDING_PERMISSION = PERMISSION + ".ignore-binding";

    static List<String> getAliases() {
        return List.of("head");
    }

    static LiteralCommandNode<CommandSourceStack> createHatCommand() {
        return Commands.literal(COMMAND_NAME)
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .executes(context -> execute(context.getSource(), HatCommand::wear))
            .then(
                Commands.literal("remove")
                    .executes(context -> execute(context.getSource(), HatCommand::remove))
            )
            .build();
    }

    private static int execute(CommandSourceStack source, Predicate<Player> action) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getSender().sendMessage(CommandMessages.HAT_PLAYER_ONLY.apply("/" + COMMAND_NAME));
            return 0;
        }

        return action.test(player) ? Command.SINGLE_SUCCESS : 0;
    }

    private static boolean wear(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();

        if (hand.isEmpty()) {
            player.sendMessage(CommandMessages.HAT_FAIL);
            return false;
        }

        if (!player.hasPermission(ALLOW_TYPE_PERMISSION_PREFIX + hand.getType().key().value())) {
            player.sendMessage(CommandMessages.HAT_PREVENTED);
            return false;
        }

        ItemStack head = inventory.getHelmet();

        if (isBindingCursed(head) && !player.hasPermission(IGNORE_BINDING_PERMISSION)) {
            player.sendMessage(CommandMessages.HAT_CURSE);
            return false;
        }

        inventory.setHelmet(hand);
        inventory.setItemInMainHand(head);
        player.sendMessage(CommandMessages.HAT_PLACED);
        return true;
    }

    private static boolean remove(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack head = inventory.getHelmet();

        if (head.isEmpty()) {
            player.sendMessage(CommandMessages.HAT_EMPTY);
            return false;
        }

        if (isBindingCursed(head) && !player.hasPermission(IGNORE_BINDING_PERMISSION)) {
            player.sendMessage(CommandMessages.HAT_CURSE);
            return false;
        }

        inventory.setHelmet(null);
        inventory.addItem(head).values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
        player.sendMessage(CommandMessages.HAT_REMOVED);
        return true;
    }

    private static boolean isBindingCursed(@Nullable ItemStack item) {
        return item != null && item.containsEnchantment(Enchantment.BINDING_CURSE);
    }
}
