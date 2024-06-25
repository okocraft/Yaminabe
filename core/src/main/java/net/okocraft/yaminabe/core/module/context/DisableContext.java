package net.okocraft.yaminabe.core.module.context;

import net.okocraft.yaminabe.core.platform.listener.ListenerRegistrar;
import org.jetbrains.annotations.NotNull;

public record DisableContext(@NotNull ListenerRegistrar listenerRegistrar) {
}
