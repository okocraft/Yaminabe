package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Collection;
import java.util.List;

@NotNullByDefault
final class ItemCommand {

    private static final String COMMAND_NAME = "item";
    private static final String PERMISSION = "yaminabe.command.item";

    private static final String ITEM_ARGUMENT = "item";
    private static final String AMOUNT_ARGUMENT = "amount";
    private static final int MAX_AMOUNT = 6400;

    static List<String> getAliases() {
        return List.of("i");
    }

    static LiteralCommandNode<CommandSourceStack> createItemCommand() {
        return Commands.literal(COMMAND_NAME)
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .then(
                Commands.argument(ITEM_ARGUMENT, ArgumentTypes.itemStack())
                    .executes(context -> give(context, 1))
                    .then(
                        Commands.argument(AMOUNT_ARGUMENT, IntegerArgumentType.integer(1, MAX_AMOUNT))
                            .executes(context -> give(context, context.getArgument(AMOUNT_ARGUMENT, Integer.class)))
                    )
            )
            .build();
    }

    private static int give(CommandContext<CommandSourceStack> context, int amount) {
        if (!(context.getSource().getExecutor() instanceof Player player)) {
            context.getSource().getSender().sendMessage(CommandMessages.ITEM_PLAYER_ONLY.apply("/" + COMMAND_NAME));
            return 0;
        }

        ItemStack item = context.getArgument(ITEM_ARGUMENT, ItemStack.class);
        int maxStackSize = item.getMaxStackSize();
        ItemStack[] stacks = new ItemStack[(amount + maxStackSize - 1) / maxStackSize];

        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = item.asQuantity(Math.min(amount - i * maxStackSize, maxStackSize));
        }

        Collection<ItemStack> leftovers = player.getInventory().addItem(stacks).values();

        if (!leftovers.isEmpty()) {
            Location location = player.getLocation();
            leftovers.forEach(leftover -> player.getWorld().dropItem(location, leftover));
        }

        player.sendMessage(CommandMessages.ITEM_GIVEN.apply(amount, item.effectiveName()));
        return Command.SINGLE_SUCCESS;
    }
}
