package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@NotNullByDefault
final class PTimeCommand {

    private static final String COMMAND_NAME = "ptime";
    private static final String PERMISSION = "yaminabe.command.ptime";
    private static final String SET_PERMISSION = PERMISSION + ".set";
    private static final String SET_OTHERS_PERMISSION = SET_PERMISSION + ".others";
    private static final String RESET_PERMISSION = PERMISSION + ".reset";
    private static final String RESET_OTHERS_PERMISSION = RESET_PERMISSION + ".others";
    private static final String QUERY_PERMISSION = PERMISSION + ".query";
    private static final String QUERY_OTHERS_PERMISSION = QUERY_PERMISSION + ".others";

    private static final String TIME_ARGUMENT = "time";
    private static final String TARGETS_ARGUMENT = "targets";
    private static final int TICKS_PER_DAY = 24000;

    private final EntityScheduler scheduler;

    private PTimeCommand(EntityScheduler scheduler) {
        this.scheduler = scheduler;
    }

    static LiteralCommandNode<CommandSourceStack> createPTimeCommand(EntityScheduler scheduler) {
        return new PTimeCommand(scheduler).build();
    }

    private LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(COMMAND_NAME)
            .requires(source -> source.getSender().hasPermission(PERMISSION))
            .then(
                Commands.literal("set")
                    .requires(source -> source.getSender().hasPermission(SET_PERMISSION))
                    .then(this.timeMarker("day", 1000))
                    .then(this.timeMarker("noon", 6000))
                    .then(this.timeMarker("night", 13000))
                    .then(this.timeMarker("midnight", 18000))
                    .then(
                        Commands.argument(TIME_ARGUMENT, ArgumentTypes.time())
                            .executes(context -> this.set(context, normalizedTime(context), self(context)))
                            .then(
                                Commands.argument(TARGETS_ARGUMENT, ArgumentTypes.players())
                                    .executes(context -> this.set(context, normalizedTime(context), resolveTargets(context)))
                            )
                    )
            )
            .then(
                Commands.literal("reset")
                    .requires(source -> source.getSender().hasPermission(RESET_PERMISSION))
                    .executes(context -> this.reset(context, self(context)))
                    .then(
                        Commands.argument(TARGETS_ARGUMENT, ArgumentTypes.players())
                            .executes(context -> this.reset(context, resolveTargets(context)))
                    )
            )
            .then(
                Commands.literal("query")
                    .requires(source -> source.getSender().hasPermission(QUERY_PERMISSION))
                    .executes(context -> this.query(context, self(context)))
                    .then(
                        Commands.argument(TARGETS_ARGUMENT, ArgumentTypes.players())
                            .executes(context -> this.query(context, resolveTargets(context)))
                    )
            )
            .build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> timeMarker(String name, int ticks) {
        return Commands.literal(name)
            .executes(context -> this.set(context, ticks, self(context)))
            .then(
                Commands.argument(TARGETS_ARGUMENT, ArgumentTypes.players())
                    .executes(context -> this.set(context, ticks, resolveTargets(context)))
            );
    }

    private static int normalizedTime(CommandContext<CommandSourceStack> context) {
        return Math.floorMod(IntegerArgumentType.getInteger(context, TIME_ARGUMENT), TICKS_PER_DAY);
    }

    private int set(CommandContext<CommandSourceStack> context, int ticks, @Nullable List<Player> targets) {
        if (targets == null || !canTargetOthers(context.getSource(), targets, SET_OTHERS_PERMISSION)) {
            return 0;
        }

        List<Player> scheduled = this.schedule(targets, player -> player.setPlayerTime(ticks, false));

        if (scheduled.isEmpty()) {
            context.getSource().getSender().sendMessage(CommandMessages.PTIME_NO_TARGETS);
            return 0;
        }

        context.getSource().getSender().sendMessage(CommandMessages.PTIME_SET.apply(ticks + "t", targetNames(scheduled)));
        return scheduled.size();
    }

    private int reset(CommandContext<CommandSourceStack> context, @Nullable List<Player> targets) {
        if (targets == null || !canTargetOthers(context.getSource(), targets, RESET_OTHERS_PERMISSION)) {
            return 0;
        }

        List<Player> scheduled = this.schedule(targets, Player::resetPlayerTime);

        if (scheduled.isEmpty()) {
            context.getSource().getSender().sendMessage(CommandMessages.PTIME_NO_TARGETS);
            return 0;
        }

        context.getSource().getSender().sendMessage(CommandMessages.PTIME_RESET.apply(targetNames(scheduled)));
        return scheduled.size();
    }

    private int query(CommandContext<CommandSourceStack> context, @Nullable List<Player> targets) {
        if (targets == null || !canTargetOthers(context.getSource(), targets, QUERY_OTHERS_PERMISSION)) {
            return 0;
        }

        CommandSender sender = context.getSource().getSender();
        int scheduled = 0;

        for (Player player : targets) {
            if (this.scheduler.execute(player, () -> sendQuery(sender, player))) {
                scheduled++;
            }
        }

        if (scheduled == 0) {
            sender.sendMessage(CommandMessages.PTIME_NO_TARGETS);
        }

        return scheduled;
    }

    private static void sendQuery(CommandSender sender, Player player) {
        long offset = player.getPlayerTimeOffset();

        if (!player.isPlayerTimeRelative()) {
            long fixedTime = Math.floorMod(offset, (long) TICKS_PER_DAY);
            sender.sendMessage(CommandMessages.PTIME_QUERY_FIXED.apply(player.getName(), fixedTime + "t"));
        } else if (offset == 0) {
            sender.sendMessage(CommandMessages.PTIME_QUERY_NORMAL.apply(player.getName()));
        } else {
            String formattedOffset = (0 < offset ? "+" : "") + offset + "t";
            sender.sendMessage(CommandMessages.PTIME_QUERY_RELATIVE.apply(player.getName(), formattedOffset));
        }
    }

    private List<Player> schedule(List<Player> targets, Consumer<Player> action) {
        List<Player> scheduled = new ArrayList<>(targets.size());

        for (Player player : targets) {
            if (this.scheduler.execute(player, () -> action.accept(player))) {
                scheduled.add(player);
            }
        }

        return scheduled;
    }

    private static @Nullable List<Player> self(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getExecutor() instanceof Player player) {
            return List.of(player);
        }

        context.getSource().getSender().sendMessage(CommandMessages.PTIME_TARGET_REQUIRED);
        return null;
    }

    private static @Nullable List<Player> resolveTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        List<Player> targets = context.getArgument(TARGETS_ARGUMENT, PlayerSelectorArgumentResolver.class).resolve(context.getSource());

        if (targets.isEmpty()) {
            context.getSource().getSender().sendMessage(CommandMessages.PTIME_NO_TARGETS);
            return null;
        }

        return targets;
    }

    static boolean canTargetOthers(CommandSourceStack source, List<Player> targets, String permission) {
        Player senderPlayer = source.getSender() instanceof Player player ? player : null;
        boolean includesOthers = senderPlayer == null || targets.stream().anyMatch(target -> !target.equals(senderPlayer));

        if (!includesOthers || source.getSender().hasPermission(permission)) {
            return true;
        }

        source.getSender().sendMessage(CommandMessages.PTIME_OTHERS_PREVENTED);
        return false;
    }

    private static String targetNames(List<Player> targets) {
        return targets.stream().map(Player::getName).collect(Collectors.joining(", "));
    }
}
