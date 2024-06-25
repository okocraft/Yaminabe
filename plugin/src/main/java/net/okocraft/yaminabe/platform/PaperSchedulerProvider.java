package net.okocraft.yaminabe.platform;

import net.okocraft.yaminabe.core.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.core.platform.scheduler.SchedulerProvider;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PaperSchedulerProvider implements SchedulerProvider {

    private final Plugin plugin;
    private final Scheduler async;

    public PaperSchedulerProvider(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.async = new PaperAsyncScheduler(plugin);
    }

    @Override
    public @NotNull Scheduler async() {
        return this.async;
    }

    @Override
    public @NotNull Scheduler entity(@NotNull Entity entity) {
        return new PapeEntityScheduler(this.plugin, entity);
    }
}
