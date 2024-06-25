package net.okocraft.yaminabe.platform;

import net.okocraft.yaminabe.core.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.core.platform.scheduler.Scheduler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class PaperAsyncScheduler implements Scheduler {

    private final Plugin plugin;

    PaperAsyncScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runNow(@NotNull Runnable task) {
        this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, ignored -> task.run());
    }

    @Override
    public void runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay cannot be negative");
        } else if (delay.isZero()) {
            this.runNow(task);
        } else {
            this.plugin.getServer().getAsyncScheduler().runDelayed(
                this.plugin,
                ignored -> task.run(),
                delay.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }
    }

    @Override
    public void runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval) {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval cannot be negative or zero");
        }
        this.plugin.getServer().getAsyncScheduler().runAtFixedRate(
            this.plugin,
            scheduledTask -> task.accept(scheduledTask::cancel),
            interval.toMillis(),
            interval.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }
}
