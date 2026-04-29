package net.okocraft.yaminabe.common.module.context;

import net.okocraft.yaminabe.common.platform.listener.ListenerRegistrar;
import org.jetbrains.annotations.NotNull;

public record DisableContext(@NotNull ListenerRegistrar listenerRegistrar) {
}
