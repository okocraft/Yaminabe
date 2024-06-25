package net.okocraft.yaminabe.core.platform.scheduler;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface SchedulerProvider {

    @NotNull
    Scheduler async();

    @NotNull
    Scheduler entity(@NotNull Entity entity);

}
