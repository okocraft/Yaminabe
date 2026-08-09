package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.text.FormatTag;
import net.okocraft.yaminabe.common.text.MiniMessageText;
import net.okocraft.yaminabe.paper.permission.PermissionCheckers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@NotNullByDefault
final class ItemNameCommand {

    private static final String COMMAND_NAME = "itemname";
    private static final String PERMISSION = "yaminabe.command.itemname";
    private static final String ALLOW_TYPE_PERMISSION_PREFIX = PERMISSION + ".allow-type.";
    private static final String IGNORE_LENGTH_LIMIT_PERMISSION = PERMISSION + ".ignore-length-limit";
    private static final String FORMAT_PERMISSION_BASE = PERMISSION + ".format";

    private static final Set<FormatTag> FORMAT_TAGS = EnumSet.of(
        FormatTag.COLOR, FormatTag.DECORATION, FormatTag.GRADIENT, FormatTag.RAINBOW,
        FormatTag.TRANSITION, FormatTag.SHADOW_COLOR, FormatTag.FONT, FormatTag.TRANSLATABLE, FormatTag.RESET
    );

    private static final String NAME_ARGUMENT = "name";

    private static final int MAX_NAME_LENGTH = 200;

    static List<String> getAliases() {
        return List.of("iname");
    }

    static LiteralCommandNode<CommandSourceStack> createItemNameCommand() {
        return Commands.literal(COMMAND_NAME)
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .executes(context -> rename(context, null))
            .then(
                Commands.argument(NAME_ARGUMENT, StringArgumentType.greedyString())
                    .suggests(ItemNameCommand::suggestCurrentName)
                    .executes(context -> rename(context, context.getArgument(NAME_ARGUMENT, String.class)))
            )
            .build();
    }

    private static CompletableFuture<Suggestions> suggestCurrentName(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (!(context.getSource().getExecutor() instanceof Player player)) {
            return builder.buildFuture();
        }

        CommandSender sender = context.getSource().getSender();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.isEmpty() || !sender.hasPermission(ALLOW_TYPE_PERMISSION_PREFIX + item.getType().key().value())) {
            return builder.buildFuture();
        }

        Component name = item.getData(DataComponentTypes.CUSTOM_NAME);

        if (name == null) {
            return builder.buildFuture();
        }

        String source = MiniMessageText.toEditableSource(PermissionCheckers.of(sender), FORMAT_PERMISSION_BASE, FORMAT_TAGS, name, MAX_NAME_LENGTH);

        if (source != null && source.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
            builder.suggest(source);
        }

        return builder.buildFuture();
    }

    private static int rename(CommandContext<CommandSourceStack> context, @Nullable String name) {
        CommandSender sender = context.getSource().getSender();

        if (!(context.getSource().getExecutor() instanceof Player player)) {
            sender.sendMessage(CommandMessages.ITEMNAME_PLAYER_ONLY.apply("/" + COMMAND_NAME));
            return 0;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.isEmpty()) {
            sender.sendMessage(CommandMessages.ITEMNAME_NO_ITEM);
            return 0;
        }

        if (!sender.hasPermission(ALLOW_TYPE_PERMISSION_PREFIX + item.getType().key().value())) {
            sender.sendMessage(CommandMessages.ITEMNAME_PREVENTED.apply(item.effectiveName()));
            return 0;
        }

        if (name == null || name.isBlank()) {
            item.resetData(DataComponentTypes.CUSTOM_NAME);
            sender.sendMessage(CommandMessages.ITEMNAME_CLEARED);
            return Command.SINGLE_SUCCESS;
        }

        if (MAX_NAME_LENGTH < name.length() && !sender.hasPermission(IGNORE_LENGTH_LIMIT_PERMISSION)) {
            sender.sendMessage(CommandMessages.ITEMNAME_TOO_LONG.apply(MAX_NAME_LENGTH));
            return 0;
        }

        Component component = MiniMessageText.withItalicOff(MiniMessageText.parse(PermissionCheckers.of(sender), FORMAT_PERMISSION_BASE, FORMAT_TAGS, name));

        item.setData(DataComponentTypes.CUSTOM_NAME, component);
        sender.sendMessage(CommandMessages.ITEMNAME_RENAMED.apply(component));
        return Command.SINGLE_SUCCESS;
    }
}
