package net.okocraft.yaminabe.platform;

import io.papermc.paper.util.Tick;
import net.okocraft.yaminabe.core.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.core.platform.scheduler.Scheduler;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

class PapeEntityScheduler implements Scheduler {

    private final Plugin plugin;
    private final Entity entity;

    PapeEntityScheduler(@NotNull Plugin plugin, @NotNull Entity entity) {
        this.plugin = plugin;
        this.entity = entity;
    }

    @Override
    public void runNow(@NotNull Runnable task) {
        this.entity.getScheduler().run(this.plugin, ignored -> task.run(), null);
    }

    @Override
    public void runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay cannot be negative");
        } else if (delay.isZero()) {
            this.runNow(task);
        } else {
            this.entity.getScheduler().runDelayed(
                this.plugin,
                ignored -> task.run(),
                null,
                Tick.tick().fromDuration(delay)
            );
        }
    }

    @Override
    public void runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval) {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval cannot be negative or zero");
        }
        int ticks = Tick.tick().fromDuration(interval);
        this.entity.getScheduler().runAtFixedRate(
            this.plugin,
            scheduledTask -> task.accept(scheduledTask::cancel),
            null,
            ticks,
            ticks
        );
    }
}
