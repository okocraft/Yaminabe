package net.okocraft.yaminabe.paper.platform;

import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.platform.scheduler.SchedulerProvider;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PaperSchedulerProvider implements SchedulerProvider {

    private final Scheduler async;

    public PaperSchedulerProvider(@NotNull Plugin plugin) {
        this.async = new PaperAsyncScheduler(plugin);
    }

    @Override
    public @NotNull Scheduler async() {
        return this.async;
    }

}
