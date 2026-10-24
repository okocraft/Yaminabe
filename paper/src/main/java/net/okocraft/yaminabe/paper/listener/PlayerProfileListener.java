package net.okocraft.yaminabe.paper.listener;

import net.okocraft.yaminabe.common.player.PlayerProfile;
import net.okocraft.yaminabe.common.player.PlayerProfileRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Clock;
import java.util.Objects;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

public final class PlayerProfileListener implements Listener {

    private final PlayerProfileRepository repository;
    private final Clock clock;

    public PlayerProfileListener(PlayerProfileRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        // Capture identity on the event thread; the database worker never accesses Player state.
        var profile = new PlayerProfile(player.getUniqueId(), player.getName(), this.clock.instant());
        this.repository.upsert(profile).whenComplete((ignored, failure) -> {
            if (failure != null) {
                log().error("Failed to persist player identity for {}", profile.uuid(), failure);
            }
        });
    }
}
