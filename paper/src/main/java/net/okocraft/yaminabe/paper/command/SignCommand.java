package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.okocraft.yaminabe.common.text.FormatTag;
import net.okocraft.yaminabe.common.text.MiniMessageText;
import net.okocraft.yaminabe.paper.permission.PermissionCheckers;
import net.okocraft.yaminabe.paper.platform.RegionScheduler;
import org.bukkit.DyeColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@NotNullByDefault
final class SignCommand {

    private static final String COMMAND_NAME = "sign";
    private static final String PERMISSION = "yaminabe.command.sign";
    private static final String AT_PERMISSION = PERMISSION + ".at";
    private static final String IGNORE_LENGTH_LIMIT_PERMISSION = PERMISSION + ".ignore-length-limit";
    private static final String IGNORE_WAXED_PERMISSION = PERMISSION + ".ignore-waxed";
    private static final String GLOWING_PERMISSION = PERMISSION + ".glowing";
    private static final String COLOR_PERMISSION = PERMISSION + ".color";
    private static final String WAXED_PERMISSION = PERMISSION + ".waxed";
    private static final String FORMAT_PERMISSION_BASE = PERMISSION + ".format";

    private static final Set<FormatTag> FORMAT_TAGS = EnumSet.of(
        FormatTag.COLOR, FormatTag.DECORATION, FormatTag.GRADIENT, FormatTag.RAINBOW,
        FormatTag.TRANSITION, FormatTag.SHADOW_COLOR, FormatTag.FONT, FormatTag.CLICK, FormatTag.TRANSLATABLE,
        FormatTag.RESET
    );

    // Kept in the order the colors are suggested in.
    private static final Map<String, DyeColor> COLORS_BY_NAME = colorsByName();

    private static final String POSITION_ARGUMENT = "position";
    private static final String LINE_ARGUMENT = "line";
    private static final String TEXT_ARGUMENT = "text";
    private static final String GLOWING_ARGUMENT = "glowing";
    private static final String COLOR_ARGUMENT = "color";
    private static final String WAXED_ARGUMENT = "waxed";

    private static final int LINES = 4;

    private static final int MAX_LINE_LENGTH = 15;

    private static final int MAX_SUGGESTED_LENGTH = 200;

    private static final int TARGET_DISTANCE = 5;

    private final RegionScheduler scheduler;
    private final SignChangeNotifier notifier;

    private SignCommand(RegionScheduler scheduler, SignChangeNotifier notifier) {
        this.scheduler = scheduler;
        this.notifier = notifier;
    }

    static List<String> getAliases() {
        return List.of("editsign");
    }

    static LiteralCommandNode<CommandSourceStack> createSignCommand(RegionScheduler scheduler) {
        return createSignCommand(scheduler, SignCommand::callSignChangeEvent, ArgumentTypes.blockPosition());
    }

    static LiteralCommandNode<CommandSourceStack> createSignCommand(RegionScheduler scheduler, SignChangeNotifier notifier, ArgumentType<BlockPositionResolver> positionArgument) {
        return new SignCommand(scheduler, notifier).build(positionArgument);
    }

    private LiteralCommandNode<CommandSourceStack> build(ArgumentType<BlockPositionResolver> positionArgument) {
        LiteralArgumentBuilder<CommandSourceStack> command =
            this.addSubcommands(Commands.literal(COMMAND_NAME), new LookedAtSign(null))
                .requires(source -> source.getSender().hasPermission(PERMISSION));

        for (Side side : Side.values()) {
            command.then(this.addSubcommands(Commands.literal(nameOf(side)), new LookedAtSign(side)));
        }

        RequiredArgumentBuilder<CommandSourceStack, BlockPositionResolver> position =
            this.addSubcommands(Commands.argument(POSITION_ARGUMENT, positionArgument), new SignAt(null));

        for (Side side : Side.values()) {
            position.then(this.addSubcommands(Commands.literal(nameOf(side)), new SignAt(side)));
        }

        return command
            .then(
                Commands.literal("at")
                    .requires(source -> source.getSender().hasPermission(AT_PERMISSION))
                    .then(position)
            )
            .build();
    }

    private <T extends ArgumentBuilder<CommandSourceStack, T>> T addSubcommands(T builder, SignFinder finder) {
        return builder
            .then(
                Commands.literal("set")
                    .then(
                        lineArgument()
                            .then(
                                this.textArgument(finder, editable(this::setLine))
                                    .suggests((context, suggestions) -> suggestCurrentLine(context, suggestions, finder))
                            )
                    )
            )
            .then(
                Commands.literal("insert")
                    .then(lineArgument().then(this.textArgument(finder, editable(this::insertLine))))
            )
            .then(
                Commands.literal("remove")
                    .then(lineArgument().executes(context -> this.edit(context, finder, editable(this::removeLine))))
            )
            .then(
                Commands.literal("clear")
                    .executes(context -> this.edit(context, finder, editable(this::clear)))
                    .then(lineArgument().executes(context -> this.edit(context, finder, editable(this::clearLine))))
            )
            .then(
                Commands.literal("glowing")
                    .requires(source -> source.getSender().hasPermission(GLOWING_PERMISSION))
                    .then(
                        Commands.argument(GLOWING_ARGUMENT, BoolArgumentType.bool())
                            .executes(context -> this.edit(context, finder, editable(SignCommand::setGlowing)))
                    )
            )
            .then(
                Commands.literal("color")
                    .requires(source -> source.getSender().hasPermission(COLOR_PERMISSION))
                    .then(
                        Commands.argument(COLOR_ARGUMENT, StringArgumentType.word())
                            .suggests(SignCommand::suggestColors)
                            .executes(context -> this.edit(context, finder, editable(SignCommand::setColor)))
                    )
            )
            .then(
                Commands.literal("waxed")
                    .requires(source -> source.getSender().hasPermission(WAXED_PERMISSION))
                    .then(
                        Commands.argument(WAXED_ARGUMENT, BoolArgumentType.bool())
                            .executes(context -> this.edit(context, finder, SignCommand::setWaxed))
                    )
            );
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> lineArgument() {
        return Commands.argument(LINE_ARGUMENT, IntegerArgumentType.integer(1, LINES)).suggests(SignCommand::suggestLineNumbers);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> textArgument(SignFinder finder, SignAction action) {
        return Commands.argument(TEXT_ARGUMENT, StringArgumentType.greedyString()).executes(context -> this.edit(context, finder, action));
    }

    private int edit(CommandContext<CommandSourceStack> context, SignFinder finder, SignAction action) throws CommandSyntaxException {
        Location location = finder.locate(context);

        if (location == null) {
            return 0;
        }

        AtomicInteger result = new AtomicInteger();

        boolean edited = this.scheduler.execute(location, () -> {
            Sign sign = finder.find(context, location);
            result.set(sign == null ? 0 : action.run(context, sign, finder.side(context, sign)));
        });

        // An edit left to another thread has nothing to report yet but that it was taken on.
        return edited ? result.get() : Command.SINGLE_SUCCESS;
    }

    private static SignAction editable(SignAction action) {
        return (context, sign, side) ->
            isEditable(context.getSource().getSender(), sign) ? action.run(context, sign, side) : 0;
    }

    private int setLine(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();
        int number = context.getArgument(LINE_ARGUMENT, Integer.class);
        Component line = parseLine(sender, context.getArgument(TEXT_ARGUMENT, String.class));

        if (!isAcceptableLength(sender, line)) {
            return 0;
        }

        List<Component> lines = new ArrayList<>(sign.getSide(side).lines());
        lines.set(number - 1, line);

        if (!this.applyLines(context, sign, side, lines)) {
            return 0;
        }

        sender.sendMessage(CommandMessages.SIGN_SET.apply(number, line));
        return Command.SINGLE_SUCCESS;
    }

    private int insertLine(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();
        int number = context.getArgument(LINE_ARGUMENT, Integer.class);
        Component line = parseLine(sender, context.getArgument(TEXT_ARGUMENT, String.class));

        if (!isAcceptableLength(sender, line)) {
            return 0;
        }

        List<Component> lines = new ArrayList<>(sign.getSide(side).lines());

        // The pushed down lines have nowhere to go if the last one is written on, and dropping it would lose text the
        // sender did not ask to lose.
        if (!isEmpty(lines.get(LINES - 1))) {
            sender.sendMessage(CommandMessages.SIGN_LAST_LINE_NOT_EMPTY.apply(LINES));
            return 0;
        }

        lines.add(number - 1, line);
        lines.remove(LINES);

        if (!this.applyLines(context, sign, side, lines)) {
            return 0;
        }

        sender.sendMessage(CommandMessages.SIGN_INSERTED.apply(number, line));
        return Command.SINGLE_SUCCESS;
    }

    private int removeLine(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();
        int number = context.getArgument(LINE_ARGUMENT, Integer.class);

        List<Component> lines = new ArrayList<>(sign.getSide(side).lines());
        lines.remove(number - 1);
        lines.add(Component.empty());

        if (!this.applyLines(context, sign, side, lines)) {
            return 0;
        }

        sender.sendMessage(CommandMessages.SIGN_REMOVED.apply(number));
        return Command.SINGLE_SUCCESS;
    }

    private int clear(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();

        if (sign.getSide(side).lines().stream().allMatch(SignCommand::isEmpty)) {
            sender.sendMessage(CommandMessages.SIGN_ALREADY_EMPTY);
            return 0;
        }

        List<Component> lines = new ArrayList<>(Collections.nCopies(LINES, Component.empty()));

        if (!this.applyLines(context, sign, side, lines)) {
            return 0;
        }

        sender.sendMessage(CommandMessages.SIGN_CLEARED);
        return Command.SINGLE_SUCCESS;
    }

    private int clearLine(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();
        int number = context.getArgument(LINE_ARGUMENT, Integer.class);
        List<Component> lines = new ArrayList<>(sign.getSide(side).lines());

        if (isEmpty(lines.get(number - 1))) {
            sender.sendMessage(CommandMessages.SIGN_LINE_ALREADY_EMPTY.apply(number));
            return 0;
        }

        lines.set(number - 1, Component.empty());

        if (!this.applyLines(context, sign, side, lines)) {
            return 0;
        }

        sender.sendMessage(CommandMessages.SIGN_CLEARED_LINE.apply(number));
        return Command.SINGLE_SUCCESS;
    }

    private static int setGlowing(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();
        boolean glowing = context.getArgument(GLOWING_ARGUMENT, Boolean.class);

        sign.getSide(side).setGlowingText(glowing);
        sign.update(true);

        sender.sendMessage(glowing ? CommandMessages.SIGN_GLOWING_ENABLED : CommandMessages.SIGN_GLOWING_DISABLED);
        return Command.SINGLE_SUCCESS;
    }

    private static int setColor(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();
        String name = context.getArgument(COLOR_ARGUMENT, String.class);
        DyeColor color = COLORS_BY_NAME.get(name.toLowerCase(Locale.ROOT));

        if (color == null) {
            sender.sendMessage(CommandMessages.SIGN_INVALID_COLOR.apply(name));
            return 0;
        }

        sign.getSide(side).setColor(color);
        sign.update(true);

        sender.sendMessage(CommandMessages.SIGN_COLOR_SET.apply(nameOf(color)));
        return Command.SINGLE_SUCCESS;
    }

    private static int setWaxed(CommandContext<CommandSourceStack> context, Sign sign, Side side) {
        CommandSender sender = context.getSource().getSender();
        boolean waxed = context.getArgument(WAXED_ARGUMENT, Boolean.class);

        // Waxing a sign is what anyone holding a honeycomb can do, while unwaxing one is undoing that.
        if (!waxed && !isEditable(sender, sign)) {
            return 0;
        }

        sign.setWaxed(waxed);
        sign.update(true);

        sender.sendMessage(waxed ? CommandMessages.SIGN_WAXED_ENABLED : CommandMessages.SIGN_WAXED_DISABLED);
        return Command.SINGLE_SUCCESS;
    }

    private boolean applyLines(CommandContext<CommandSourceStack> context, Sign sign, Side side, List<Component> lines) {
        List<Component> written = lines;

        if (context.getSource().getExecutor() instanceof Player player) {
            written = this.notifier.notifyChange(sign, player, lines, side);

            if (written == null) {
                context.getSource().getSender().sendMessage(CommandMessages.SIGN_EDIT_PREVENTED);
                return false;
            }
        }

        SignSide target = sign.getSide(side);

        for (int line = 0; line < LINES; line++) {
            Component text = line < written.size() ? written.get(line) : null;
            target.line(line, text != null ? text : Component.empty());
        }

        sign.update(true);
        return true;
    }

    private static boolean isEditable(CommandSender sender, Sign sign) {
        if (!sign.isWaxed() || sender.hasPermission(IGNORE_WAXED_PERMISSION)) {
            return true;
        }

        sender.sendMessage(CommandMessages.SIGN_IS_WAXED);
        return false;
    }

    private static boolean isAcceptableLength(CommandSender sender, Component line) {
        if (plainTextOf(line).length() <= MAX_LINE_LENGTH || sender.hasPermission(IGNORE_LENGTH_LIMIT_PERMISSION)) {
            return true;
        }

        sender.sendMessage(CommandMessages.SIGN_TOO_LONG.apply(MAX_LINE_LENGTH));
        return false;
    }

    private static Component parseLine(CommandSender sender, String text) {
        return MiniMessageText.parse(PermissionCheckers.of(sender), FORMAT_PERMISSION_BASE, FORMAT_TAGS, text);
    }

    private static boolean isEmpty(Component line) {
        return plainTextOf(line).isEmpty();
    }

    private static String plainTextOf(Component line) {
        return PlainTextComponentSerializer.plainText().serialize(line);
    }

    private static Map<String, DyeColor> colorsByName() {
        Map<String, DyeColor> colors = new LinkedHashMap<>();

        for (DyeColor color : DyeColor.values()) {
            colors.put(nameOf(color), color);
        }

        return Collections.unmodifiableMap(colors);
    }

    private static String nameOf(DyeColor color) {
        return color.name().toLowerCase(Locale.ROOT);
    }

    private static String nameOf(Side side) {
        return side.name().toLowerCase(Locale.ROOT);
    }

    private static CompletableFuture<Suggestions> suggestLineNumbers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (int number = 1; number <= LINES; number++) {
            if (String.valueOf(number).startsWith(builder.getRemaining())) {
                builder.suggest(number);
            }
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();

        for (String name : COLORS_BY_NAME.keySet()) {
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestCurrentLine(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, SignFinder finder) {
        Sign sign = finder.findToSuggestFor(context);

        if (sign == null) {
            return builder.buildFuture();
        }

        CommandSender sender = context.getSource().getSender();
        Component line = sign.getSide(finder.side(context, sign)).line(context.getArgument(LINE_ARGUMENT, Integer.class) - 1);
        String source = MiniMessageText.toEditableSource(PermissionCheckers.of(sender), FORMAT_PERMISSION_BASE, FORMAT_TAGS, line, MAX_SUGGESTED_LENGTH);

        if (source != null && source.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
            builder.suggest(source);
        }

        return builder.buildFuture();
    }

    private static @Nullable Sign signLookedAtBy(Player player) {
        Location eye = player.getEyeLocation();
        // A sign has no collision shape, so the ray has to be told not to pass through the blocks that have none.
        RayTraceResult result = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), TARGET_DISTANCE, FluidCollisionMode.NEVER, false);
        Block hit = result != null ? result.getHitBlock() : null;

        return hit != null && hit.getState() instanceof Sign sign ? sign : null;
    }

    private interface SignFinder {

        @Nullable Location locate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;

        @Nullable Sign find(CommandContext<CommandSourceStack> context, Location location);

        Side side(CommandContext<CommandSourceStack> context, Sign sign);

        /**
         * Returns the sign whose current line may be suggested as the text to edit, or {@code null} for a finder that
         * has none to offer, as a sign named by its position is not worth reaching for on every keystroke.
         */
        default @Nullable Sign findToSuggestFor(CommandContext<CommandSourceStack> context) {
            return null;
        }
    }

    private record LookedAtSign(@Nullable Side side) implements SignFinder {

        @Override
        public @Nullable Location locate(CommandContext<CommandSourceStack> context) {
            if (context.getSource().getExecutor() instanceof Player player) {
                return player.getLocation();
            }

            context.getSource().getSender().sendMessage(CommandMessages.SIGN_PLAYER_ONLY.apply("/" + COMMAND_NAME));
            return null;
        }

        @Override
        public @Nullable Sign find(CommandContext<CommandSourceStack> context, Location location) {
            Sign sign = this.findToSuggestFor(context);

            if (sign == null) {
                context.getSource().getSender().sendMessage(CommandMessages.SIGN_NOT_LOOKED_AT);
            }

            return sign;
        }

        @Override
        public Side side(CommandContext<CommandSourceStack> context, Sign sign) {
            if (this.side != null) {
                return this.side;
            }

            return context.getSource().getExecutor() instanceof Player player ? sign.getInteractableSideFor(player) : Side.FRONT;
        }

        @Override
        public @Nullable Sign findToSuggestFor(CommandContext<CommandSourceStack> context) {
            return context.getSource().getExecutor() instanceof Player player ? signLookedAtBy(player) : null;
        }
    }

    private record SignAt(@Nullable Side side) implements SignFinder {

        @Override
        public @Nullable Location locate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            CommandSourceStack source = context.getSource();
            BlockPosition position = context.getArgument(POSITION_ARGUMENT, BlockPositionResolver.class).resolve(source);
            World world = source.getLocation().getWorld();

            // The chunk is left as it is rather than loaded, so a sign that is not there to be seen is not edited.
            if (!world.isChunkLoaded(position.blockX() >> 4, position.blockZ() >> 4)) {
                source.getSender().sendMessage(CommandMessages.SIGN_CHUNK_NOT_LOADED.apply(position.blockX(), position.blockY(), position.blockZ()));
                return null;
            }

            return new Location(world, position.blockX(), position.blockY(), position.blockZ());
        }

        @Override
        public @Nullable Sign find(CommandContext<CommandSourceStack> context, Location location) {
            if (location.getBlock().getState() instanceof Sign sign) {
                return sign;
            }

            context.getSource().getSender().sendMessage(
                CommandMessages.SIGN_NOT_FOUND.apply(location.getBlockX(), location.getBlockY(), location.getBlockZ())
            );
            return null;
        }

        @Override
        public Side side(CommandContext<CommandSourceStack> context, Sign sign) {
            return this.side != null ? this.side : Side.FRONT;
        }
    }

    @FunctionalInterface
    private interface SignAction {

        int run(CommandContext<CommandSourceStack> context, Sign sign, Side side);
    }

    private static @Nullable List<Component> callSignChangeEvent(Sign sign, Player player, List<Component> lines, Side side) {
        SignChangeEvent event = new SignChangeEvent(sign.getBlock(), player, lines, side);
        return event.callEvent() ? event.lines() : null;
    }

    /**
     * Lets a plugin listening for {@link SignChangeEvent} have its say on lines that are about to be written, which
     * the real command does by calling that event and a test does without a server to call it on.
     */
    @FunctionalInterface
    interface SignChangeNotifier {

        /**
         * Returns the lines to write, which the listeners may have rewritten, or {@code null} if they cancelled the
         * change.
         */
        @Nullable List<Component> notifyChange(Sign sign, Player player, List<Component> lines, Side side);
    }
}
