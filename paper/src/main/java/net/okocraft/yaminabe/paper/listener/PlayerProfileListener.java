package net.okocraft.yaminabe.paper.listener;

import net.okocraft.yaminabe.common.player.PlayerProfileService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Collections;
import java.util.IdentityHashMap;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

public final class PlayerProfileListener implements Listener {

    private final PlayerProfileService service;
    // Player identity distinguishes a retiring connection from a reconnect with the same UUID.
    private final Map<Player, PlayerProfileService.Session> connections = Collections.synchronizedMap(new IdentityHashMap<>());

    public PlayerProfileListener(PlayerProfileService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var session = this.service.join(player.getUniqueId(), player.getName());
        this.connections.put(player, session);
        session.ready().whenComplete((profile, failure) -> {
            if (failure != null) {
                log().error("Failed to record player join for {}", session.playerId(), failure);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var session = this.connections.remove(event.getPlayer());
        if (session != null) {
            this.service.quit(session).whenComplete((updated, failure) -> {
                if (failure != null) {
                    log().error("Failed to record player quit for {}", session.playerId(), failure);
                }
            });
        }
    }
}
