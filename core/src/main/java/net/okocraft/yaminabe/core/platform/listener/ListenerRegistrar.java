package net.okocraft.yaminabe.core.platform.listener;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public interface ListenerRegistrar {

    void register(@NotNull Listener listener);

    void unregister(@NotNull Listener listener);

}
