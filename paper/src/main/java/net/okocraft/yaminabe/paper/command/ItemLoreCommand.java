package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.text.FormatTag;
import net.okocraft.yaminabe.common.text.MiniMessageText;
import net.okocraft.yaminabe.paper.permission.PermissionCheckers;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@NotNullByDefault
final class ItemLoreCommand {

    private static final String COMMAND_NAME = "itemlore";
    private static final String PERMISSION = "yaminabe.command.itemlore";
    private static final String ALLOW_TYPE_PERMISSION_PREFIX = PERMISSION + ".allow-type.";
    private static final String IGNORE_LENGTH_LIMIT_PERMISSION = PERMISSION + ".ignore-length-limit";
    private static final String IGNORE_LINE_LIMIT_PERMISSION = PERMISSION + ".ignore-line-limit";
    private static final String FORMAT_PERMISSION_BASE = PERMISSION + ".format";

    private static final Set<FormatTag> FORMAT_TAGS = EnumSet.of(
        FormatTag.COLOR, FormatTag.DECORATION, FormatTag.GRADIENT, FormatTag.RAINBOW,
        FormatTag.TRANSITION, FormatTag.SHADOW_COLOR, FormatTag.FONT, FormatTag.TRANSLATABLE, FormatTag.RESET
    );

    private static final String LINE_ARGUMENT = "line";
    private static final String TEXT_ARGUMENT = "text";

    private static final int MAX_TEXT_LENGTH = 200;
    private static final int MAX_LINES = 10;

    /**
     * The number of lines the lore data component itself accepts, which cannot be exceeded even with
     * {@link #IGNORE_LINE_LIMIT_PERMISSION}, as the component rejects a longer lore with an exception.
     */
    private static final int MAX_LINES_OF_COMPONENT = 256;

    static List<String> getAliases() {
        return List.of("lore", "ilore");
    }

    static LiteralCommandNode<CommandSourceStack> createItemLoreCommand() {
        return Commands.literal(COMMAND_NAME)
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument(TEXT_ARGUMENT, StringArgumentType.greedyString())
                            .executes(ItemLoreCommand::add)
                    )
            )
            .then(
                Commands.literal("set")
                    .then(
                        Commands.argument(LINE_ARGUMENT, IntegerArgumentType.integer(1))
                            .suggests((context, builder) -> suggestLineNumbers(context, builder, 0))
                            .then(
                                Commands.argument(TEXT_ARGUMENT, StringArgumentType.greedyString())
                                    .suggests(ItemLoreCommand::suggestCurrentLine)
                                    .executes(ItemLoreCommand::set)
                            )
                    )
            )
            .then(
                Commands.literal("insert")
                    .then(
                        Commands.argument(LINE_ARGUMENT, IntegerArgumentType.integer(1))
                            .suggests((context, builder) -> suggestLineNumbers(context, builder, 1))
                            .then(
                                Commands.argument(TEXT_ARGUMENT, StringArgumentType.greedyString())
                                    .executes(ItemLoreCommand::insert)
                            )
                    )
            )
            .then(
                Commands.literal("remove")
                    .then(
                        Commands.argument(LINE_ARGUMENT, IntegerArgumentType.integer(1))
                            .suggests((context, builder) -> suggestLineNumbers(context, builder, 0))
                            .executes(ItemLoreCommand::remove)
                    )
            )
            .then(Commands.literal("clear").executes(ItemLoreCommand::clear))
            .build();
    }

    private static int add(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ItemStack item = getEditableItem(context);

        if (item == null) {
            return 0;
        }

        List<Component> lines = new ArrayList<>(getLines(item));
        String text = context.getArgument(TEXT_ARGUMENT, String.class);

        if (!canAddLine(sender, lines.size()) || !isAcceptableLength(sender, text)) {
            return 0;
        }

        Component line = parseLine(sender, text);
        lines.add(line);

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        sender.sendMessage(CommandMessages.ITEMLORE_ADDED.apply(line));
        return Command.SINGLE_SUCCESS;
    }

    private static int set(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ItemStack item = getEditableItem(context);

        if (item == null) {
            return 0;
        }

        List<Component> lines = new ArrayList<>(getLines(item));
        int lineNumber = context.getArgument(LINE_ARGUMENT, Integer.class);
        String text = context.getArgument(TEXT_ARGUMENT, String.class);

        if (!hasLine(sender, lines, lineNumber) || !isAcceptableLength(sender, text)) {
            return 0;
        }

        Component line = parseLine(sender, text);
        lines.set(lineNumber - 1, line);

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        sender.sendMessage(CommandMessages.ITEMLORE_SET.apply(lineNumber, line));
        return Command.SINGLE_SUCCESS;
    }

    private static int insert(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ItemStack item = getEditableItem(context);

        if (item == null) {
            return 0;
        }

        List<Component> lines = new ArrayList<>(getLines(item));
        int lineNumber = context.getArgument(LINE_ARGUMENT, Integer.class);
        String text = context.getArgument(TEXT_ARGUMENT, String.class);

        if (lines.size() + 1 < lineNumber) {
            sender.sendMessage(CommandMessages.ITEMLORE_LINE_OUT_OF_RANGE.apply(lines.size() + 1));
            return 0;
        }

        if (!canAddLine(sender, lines.size()) || !isAcceptableLength(sender, text)) {
            return 0;
        }

        Component line = parseLine(sender, text);
        lines.add(lineNumber - 1, line);

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        sender.sendMessage(CommandMessages.ITEMLORE_INSERTED.apply(lineNumber, line));
        return Command.SINGLE_SUCCESS;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ItemStack item = getEditableItem(context);

        if (item == null) {
            return 0;
        }

        List<Component> lines = new ArrayList<>(getLines(item));
        int lineNumber = context.getArgument(LINE_ARGUMENT, Integer.class);

        if (!hasLine(sender, lines, lineNumber)) {
            return 0;
        }

        lines.remove(lineNumber - 1);

        if (lines.isEmpty()) {
            item.resetData(DataComponentTypes.LORE);
        } else {
            item.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        }

        sender.sendMessage(CommandMessages.ITEMLORE_REMOVED.apply(lineNumber));
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ItemStack item = getEditableItem(context);

        if (item == null) {
            return 0;
        }

        if (getLines(item).isEmpty()) {
            sender.sendMessage(CommandMessages.ITEMLORE_NO_LORE);
            return 0;
        }

        item.resetData(DataComponentTypes.LORE);
        sender.sendMessage(CommandMessages.ITEMLORE_CLEARED);
        return Command.SINGLE_SUCCESS;
    }

    private static @Nullable ItemStack getEditableItem(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        if (!(context.getSource().getExecutor() instanceof Player player)) {
            sender.sendMessage(CommandMessages.ITEMLORE_PLAYER_ONLY.apply("/" + COMMAND_NAME));
            return null;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.isEmpty()) {
            sender.sendMessage(CommandMessages.ITEMLORE_NO_ITEM);
            return null;
        }

        if (!sender.hasPermission(ALLOW_TYPE_PERMISSION_PREFIX + item.getType().key().value())) {
            sender.sendMessage(CommandMessages.ITEMLORE_PREVENTED.apply(item.effectiveName()));
            return null;
        }

        return item;
    }

    private static List<Component> getLines(ItemStack item) {
        ItemLore lore = item.getData(DataComponentTypes.LORE);
        return lore != null ? lore.lines() : List.of();
    }

    private static boolean hasLine(CommandSender sender, List<Component> lines, int lineNumber) {
        if (lines.isEmpty()) {
            sender.sendMessage(CommandMessages.ITEMLORE_NO_LORE);
            return false;
        }

        if (lines.size() < lineNumber) {
            sender.sendMessage(CommandMessages.ITEMLORE_NO_LINE.apply(lineNumber));
            return false;
        }

        return true;
    }

    private static boolean canAddLine(CommandSender sender, int currentLines) {
        int limit = sender.hasPermission(IGNORE_LINE_LIMIT_PERMISSION) ? MAX_LINES_OF_COMPONENT : MAX_LINES;

        if (currentLines < limit) {
            return true;
        }

        sender.sendMessage(CommandMessages.ITEMLORE_TOO_MANY_LINES.apply(limit));
        return false;
    }

    private static boolean isAcceptableLength(CommandSender sender, String text) {
        if (text.length() <= MAX_TEXT_LENGTH || sender.hasPermission(IGNORE_LENGTH_LIMIT_PERMISSION)) {
            return true;
        }

        sender.sendMessage(CommandMessages.ITEMLORE_TOO_LONG.apply(MAX_TEXT_LENGTH));
        return false;
    }

    private static Component parseLine(CommandSender sender, String text) {
        return MiniMessageText.withItalicOff(MiniMessageText.parse(PermissionCheckers.of(sender), FORMAT_PERMISSION_BASE, FORMAT_TAGS, text));
    }

    private static CompletableFuture<Suggestions> suggestLineNumbers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, int additionalLines) {
        ItemStack item = getHeldItemToSuggestFor(context);

        if (item == null) {
            return builder.buildFuture();
        }

        int lines = getLines(item).size() + additionalLines;
        String remaining = builder.getRemaining();

        for (int lineNumber = 1; lineNumber <= lines; lineNumber++) {
            if (String.valueOf(lineNumber).startsWith(remaining)) {
                builder.suggest(lineNumber);
            }
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestCurrentLine(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ItemStack item = getHeldItemToSuggestFor(context);

        if (item == null) {
            return builder.buildFuture();
        }

        List<Component> lines = getLines(item);
        int lineNumber = context.getArgument(LINE_ARGUMENT, Integer.class);

        if (lines.size() < lineNumber) {
            return builder.buildFuture();
        }

        CommandSender sender = context.getSource().getSender();
        String source = MiniMessageText.toEditableSource(PermissionCheckers.of(sender), FORMAT_PERMISSION_BASE, FORMAT_TAGS, lines.get(lineNumber - 1), MAX_TEXT_LENGTH);

        if (source != null && source.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
            builder.suggest(source);
        }

        return builder.buildFuture();
    }

    private static @Nullable ItemStack getHeldItemToSuggestFor(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getExecutor() instanceof Player player)) {
            return null;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.isEmpty() || !context.getSource().getSender().hasPermission(ALLOW_TYPE_PERMISSION_PREFIX + item.getType().key().value())) {
            return null;
        }

        return item;
    }
}
