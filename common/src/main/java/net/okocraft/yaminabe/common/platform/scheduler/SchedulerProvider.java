package net.okocraft.yaminabe.common.platform.scheduler;

import org.jetbrains.annotations.NotNull;

public interface SchedulerProvider {

    @NotNull
    Scheduler async();

}
