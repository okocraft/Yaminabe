package net.okocraft.yaminabe.common.module.context;

import net.okocraft.yaminabe.common.platform.listener.ListenerRegistrar;
import net.okocraft.yaminabe.common.platform.scheduler.SchedulerProvider;
import org.jetbrains.annotations.NotNull;

public record EnableContext(@NotNull ListenerRegistrar listenerRegistrar, @NotNull SchedulerProvider schedulers) {
}
