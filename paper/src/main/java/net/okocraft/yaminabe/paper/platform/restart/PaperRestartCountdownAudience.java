package net.okocraft.yaminabe.paper.platform.restart;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownAudience;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

@NotNullByDefault
public final class PaperRestartCountdownAudience implements RestartCountdownAudience {

    private final Plugin plugin;
    private final Server server;
    private final EntityScheduler entityScheduler;
    private final AtomicReference<BossBar> activeBossBar = new AtomicReference<>();

    public PaperRestartCountdownAudience(Plugin plugin, EntityScheduler entityScheduler) {
        this.plugin = Objects.requireNonNull(plugin);
        this.server = Objects.requireNonNull(plugin.getServer());
        this.entityScheduler = Objects.requireNonNull(entityScheduler);
    }

    @Override
    public void showBossBar(BossBar bossBar) {
        Objects.requireNonNull(bossBar);
        this.activeBossBar.set(bossBar);
        this.forEachPlayer(player -> {
            if (this.activeBossBar.get() == bossBar) {
                player.showBossBar(bossBar);
            }
        });
    }

    @Override
    public void hideBossBar(BossBar bossBar) {
        Objects.requireNonNull(bossBar);
        this.activeBossBar.compareAndSet(bossBar, null);
        if (!this.plugin.isEnabled()) {
            this.hideBossBarDuringDisable(bossBar);
            return;
        }
        this.forEachPlayer(player -> player.hideBossBar(bossBar));
    }

    @Override
    public void sendMessage(Component message) {
        Objects.requireNonNull(message);
        this.forEachPlayer(player -> player.sendMessage(message));
    }

    private void hideBossBarDuringDisable(BossBar bossBar) {
        List<Player> players;
        try {
            players = List.copyOf(this.server.getOnlinePlayers());
        } catch (RuntimeException exception) {
            log().warn("Failed to collect players while removing the restart countdown BossBar during plugin disable", exception);
            return;
        }

        for (Player player : players) {
            try {
                player.hideBossBar(bossBar);
            } catch (RuntimeException exception) {
                log().warn("Failed to remove the restart countdown BossBar from player {} during plugin disable", player.getName(), exception);
            }
        }
    }

    private void forEachPlayer(Consumer<Player> action) {
        try {
            this.server.getGlobalRegionScheduler().execute(this.plugin, () -> {
                for (Player player : this.server.getOnlinePlayers()) {
                    try {
                        this.entityScheduler.execute(player, () -> {
                            try {
                                action.accept(player);
                            } catch (RuntimeException exception) {
                                log().warn("Failed to present restart countdown to player {}", player.getName(), exception);
                            }
                        });
                    } catch (RuntimeException exception) {
                        log().warn("Failed to schedule restart countdown presentation for player {}", player.getName(), exception);
                    }
                }
            });
        } catch (RuntimeException exception) {
            log().warn("Failed to schedule restart countdown presentation on the global region", exception);
        }
    }
}
