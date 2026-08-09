package net.okocraft.yaminabe.paper.platform;

import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.platform.scheduler.SchedulerProvider;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PaperSchedulerProvider implements SchedulerProvider {

    private final Scheduler async;
    private final RegionScheduler region;

    public PaperSchedulerProvider(@NotNull Plugin plugin) {
        this.async = new PaperAsyncScheduler(plugin);
        this.region = new PaperRegionScheduler(plugin);
    }

    @Override
    public @NotNull Scheduler async() {
        return this.async;
    }

    /**
     * Returns the scheduler that runs a task on the thread owning a location.
     * <p>
     * This is not part of {@link SchedulerProvider}, as a location is a platform type.
     *
     * @return the region scheduler
     */
    public @NotNull RegionScheduler region() {
        return this.region;
    }

}
