package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.okocraft.yaminabe.common.player.PlayerProfile;
import net.okocraft.yaminabe.common.player.PlayerProfileService;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

final class SeenCommand {

    private static final String PERMISSION = "yaminabe.command.seen";

    static LiteralCommandNode<CommandSourceStack> createSeenCommand(
        PlayerProfileService service,
        EntityScheduler scheduler,
        Function<UUID, Player> onlinePlayer
    ) {
        return Commands.literal("seen")
            .requires(source -> supported(source.getSender()) && source.getSender().hasPermission(PERMISSION))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    String input = StringArgumentType.getString(context, "player");
                    lookup(service, input).whenComplete((profiles, failure) -> reply(sender, scheduler, () -> {
                        if (!sender.hasPermission(PERMISSION)) {
                            return;
                        }
                        if (failure != null) {
                            log().error("Failed to query player profile", failure);
                            sender.sendMessage(CommandMessages.SEEN_FAILED);
                        } else if (profiles.isEmpty()) {
                            sender.sendMessage(CommandMessages.SEEN_NOT_FOUND.apply(input));
                        } else if (profiles.size() > 1) {
                            sender.sendMessage(CommandMessages.SEEN_AMBIGUOUS.apply(input));
                            for (PlayerProfile profile : profiles) {
                                sender.sendMessage(CommandMessages.SEEN_IDENTITY.apply(profile.lastKnownName(), profile.uuid().toString()));
                            }
                        } else {
                            refresh(sender, service, scheduler, onlinePlayer, profiles.getFirst().uuid(), input);
                        }
                    }));
                    return Command.SINGLE_SUCCESS;
                }))
            .build();
    }

    private static CompletableFuture<List<PlayerProfile>> lookup(PlayerProfileService service, String input) {
        try {
            UUID uuid = UUID.fromString(input);
            if (uuid.toString().equalsIgnoreCase(input)) {
                return service.find(uuid).thenApply(profile -> profile.stream().toList());
            }
        } catch (IllegalArgumentException ignored) {
            // Not a canonical UUID: treat it as an exact last-known account name.
        }
        return service.findByName(input);
    }

    private static void reply(CommandSender sender, EntityScheduler scheduler, Runnable action) {
        if (sender instanceof Player player) {
            // No Player access from the database worker. A reply to a departed connection is discarded.
            scheduler.execute(player, () -> {
                if (player.isOnline()) {
                    action.run();
                }
            }, () -> { });
        } else if (sender instanceof ConsoleCommandSender) {
            action.run();
        }
    }

    private static boolean supported(CommandSender sender) {
        return sender instanceof Player || sender instanceof ConsoleCommandSender;
    }

    private static void refresh(
        CommandSender sender,
        PlayerProfileService service,
        EntityScheduler scheduler,
        Function<UUID, Player> onlinePlayer,
        UUID uuid,
        String input
    ) {
        service.observe(uuid).whenComplete((observation, failure) -> {
            if (failure != null) {
                log().error("Failed to refresh player profile", failure);
            }
            reply(sender, scheduler, () -> {
                if (!sender.hasPermission(PERMISSION)) {
                    return;
                }
                if (failure != null) {
                    sender.sendMessage(CommandMessages.SEEN_FAILED);
                } else if (observation.isEmpty()) {
                    sender.sendMessage(CommandMessages.SEEN_NOT_FOUND.apply(input));
                } else {
                    var snapshot = observation.orElseThrow();
                    Player target = onlinePlayer.apply(uuid);
                    boolean visible = !(sender instanceof Player viewer) || target == null || viewer.canSee(target);
                    // Validate after the entity-scheduler delay and visibility lookup. Render exclusively from
                    // this observation, never combine old stored times with a new isOnline() result.
                    if (!service.isCurrent(snapshot)) {
                        refresh(sender, service, scheduler, onlinePlayer, uuid, input);
                        return;
                    }
                    show(sender, snapshot, target != null && visible);
                }
            });
        });
    }

    private static void show(CommandSender sender, PlayerProfileService.Observation observation, boolean visibleOnline) {
        PlayerProfile profile = observation.profile();
        String name = profile.lastKnownName();
        sender.sendMessage(CommandMessages.SEEN_IDENTITY.apply(name, profile.uuid().toString()));
        if (observation.online() && visibleOnline) {
            sender.sendMessage(CommandMessages.SEEN_ONLINE.apply(name, profile.lastLoginAt().toString()));
        } else if (profile.logoutConfirmed()) {
            sender.sendMessage(CommandMessages.SEEN_OFFLINE.apply(name, profile.lastLogoutAt().toString()));
        } else {
            sender.sendMessage(CommandMessages.SEEN_UNCONFIRMED.apply(name, profile.lastLoginAt().toString()));
        }
    }

    private SeenCommand() {
        throw new UnsupportedOperationException();
    }
}
