package net.okocraft.yaminabe.paper.platform;

import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
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
    public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay cannot be negative");
        }

        var scheduler = this.plugin.getServer().getAsyncScheduler();
        var scheduledTask = delay.isZero()
            ? scheduler.runNow(this.plugin, ignored -> task.run())
            : scheduler.runDelayed(
                this.plugin,
                ignored -> task.run(),
                delay.toMillis(),
                TimeUnit.MILLISECONDS
            );
        return scheduledTask::cancel;
    }

    @Override
    public @NotNull CancellableTask runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval) {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval cannot be negative or zero");
        }
        var scheduledTask = this.plugin.getServer().getAsyncScheduler().runAtFixedRate(
            this.plugin,
            taskHandle -> task.accept(taskHandle::cancel),
            interval.toMillis(),
            interval.toMillis(),
            TimeUnit.MILLISECONDS
        );
        return scheduledTask::cancel;
    }
}
