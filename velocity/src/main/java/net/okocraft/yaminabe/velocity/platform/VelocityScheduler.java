package net.okocraft.yaminabe.velocity.platform;

import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.Duration;
import java.util.function.Consumer;

@NotNullByDefault
public final class VelocityScheduler implements net.okocraft.yaminabe.common.platform.scheduler.Scheduler {

    private final com.velocitypowered.api.scheduler.Scheduler scheduler;
    private final Object plugin;

    public VelocityScheduler(com.velocitypowered.api.scheduler.Scheduler scheduler, Object plugin) {
        this.scheduler = scheduler;
        this.plugin = plugin;
    }

    @Override
    public void runNow(Runnable task) {
        this.scheduler.buildTask(this.plugin, task).schedule();
    }

    @Override
    public void runDelayed(Runnable task, Duration delay) {
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay cannot be negative");
        } else if (delay.isZero()) {
            this.runNow(task);
        } else {
            this.scheduler.buildTask(this.plugin, task).delay(delay).schedule();
        }
    }

    @Override
    public void runAtFixedRate(Consumer<CancellableTask> task, Duration interval) {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval cannot be negative or zero");
        }
        this.scheduler.buildTask(this.plugin, scheduledTask -> task.accept(scheduledTask::cancel))
            .delay(interval)
            .repeat(interval)
            .schedule();
    }
}
