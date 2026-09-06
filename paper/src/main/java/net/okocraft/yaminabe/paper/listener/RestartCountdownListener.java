package net.okocraft.yaminabe.paper.listener;

import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownPresenter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

@NotNullByDefault
public final class RestartCountdownListener implements Listener {

    private final RestartCountdownPresenter presenter;

    public RestartCountdownListener(RestartCountdownPresenter presenter) {
        this.presenter = Objects.requireNonNull(presenter);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.presenter.showTo(event.getPlayer());
    }
}
