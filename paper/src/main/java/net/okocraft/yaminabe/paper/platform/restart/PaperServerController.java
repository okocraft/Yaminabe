package net.okocraft.yaminabe.paper.platform.restart;

import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.execution.ServerController;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@NotNullByDefault
public final class PaperServerController implements ServerController {

    private final Plugin plugin;
    private final Server server;
    private final EntityScheduler entityScheduler;

    public PaperServerController(Plugin plugin, EntityScheduler entityScheduler) {
        this.plugin = Objects.requireNonNull(plugin);
        this.server = Objects.requireNonNull(plugin.getServer());
        this.entityScheduler = Objects.requireNonNull(entityScheduler);
    }

    @Override
    public CompletionStage<Boolean> dispatchConsoleCommand(String command) {
        Objects.requireNonNull(command);
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        try {
            this.server.getGlobalRegionScheduler().execute(this.plugin, () -> {
                try {
                    result.complete(this.server.dispatchCommand(this.server.getConsoleSender(), command));
                } catch (RuntimeException exception) {
                    result.completeExceptionally(exception);
                }
            });
        } catch (RuntimeException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }

    @Override
    public CompletionStage<Void> kickAll(Component reason) {
        Objects.requireNonNull(reason);
        List<Player> players = List.copyOf(this.server.getOnlinePlayers());
        List<CompletableFuture<Void>> kicks = new ArrayList<>(players.size());
        for (Player player : players) {
            CompletableFuture<Void> kicked = new CompletableFuture<>();
            kicks.add(kicked);
            try {
                boolean scheduled = this.entityScheduler.execute(
                    player,
                    () -> {
                        try {
                            player.kick(reason);
                            kicked.complete(null);
                        } catch (RuntimeException exception) {
                            kicked.completeExceptionally(exception);
                        }
                    },
                    () -> kicked.complete(null)
                );
                if (!scheduled) {
                    kicked.complete(null);
                }
            } catch (RuntimeException exception) {
                kicked.completeExceptionally(exception);
            }
        }
        return CompletableFuture.allOf(kicks.toArray(CompletableFuture[]::new));
    }

    @Override
    public void stop() {
        this.server.getGlobalRegionScheduler().execute(this.plugin, this.server::shutdown);
    }

    @Override
    public void restart() {
        this.server.getGlobalRegionScheduler().execute(this.plugin, this.server::restart);
    }
}
