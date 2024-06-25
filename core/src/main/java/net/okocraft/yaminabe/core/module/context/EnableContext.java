package net.okocraft.yaminabe.core.module.context;

import net.okocraft.yaminabe.core.platform.listener.ListenerRegistrar;
import net.okocraft.yaminabe.core.platform.scheduler.SchedulerProvider;
import org.jetbrains.annotations.NotNull;

public record EnableContext(@NotNull ListenerRegistrar listenerRegistrar, @NotNull SchedulerProvider schedulers) {
}
