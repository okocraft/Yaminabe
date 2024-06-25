package net.okocraft.yaminabe.platform;

import net.okocraft.yaminabe.core.platform.listener.ListenerRegistrar;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PaperListenerRegistrar implements ListenerRegistrar {

    private final Plugin plugin;

    public PaperListenerRegistrar(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(@NotNull Listener listener) {
        this.plugin.getServer().getPluginManager().registerEvents(listener, this.plugin);
    }

    @Override
    public void unregister(@NotNull Listener listener) {
        HandlerList.unregisterAll(listener);
    }
}
