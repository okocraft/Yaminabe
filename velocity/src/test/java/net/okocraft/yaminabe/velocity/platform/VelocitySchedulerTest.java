package net.okocraft.yaminabe.velocity.platform;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.function.Consumer;

class VelocitySchedulerTest {

    @Test
    void testDelayedTaskCanBeCancelled() {
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        Scheduler.TaskBuilder builder = Mockito.mock(Scheduler.TaskBuilder.class);
        ScheduledTask scheduledTask = Mockito.mock(ScheduledTask.class);
        Object plugin = new Object();
        Duration delay = Duration.ofSeconds(1);
        Mockito.when(scheduler.buildTask(Mockito.eq(plugin), Mockito.any(Runnable.class))).thenReturn(builder);
        Mockito.when(builder.delay(delay)).thenReturn(builder);
        Mockito.when(builder.schedule()).thenReturn(scheduledTask);

        var velocityScheduler = new VelocityScheduler(scheduler, plugin);
        var task = velocityScheduler.runDelayed(() -> {
        }, delay);
        task.cancel();

        Mockito.verify(builder).delay(delay);
        Mockito.verify(scheduledTask).cancel();
    }

    @Test
    void testZeroDelayTaskCanBeCancelled() {
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        Scheduler.TaskBuilder builder = Mockito.mock(Scheduler.TaskBuilder.class);
        ScheduledTask scheduledTask = Mockito.mock(ScheduledTask.class);
        Object plugin = new Object();
        Mockito.when(scheduler.buildTask(Mockito.eq(plugin), Mockito.any(Runnable.class))).thenReturn(builder);
        Mockito.when(builder.schedule()).thenReturn(scheduledTask);

        var velocityScheduler = new VelocityScheduler(scheduler, plugin);
        var task = velocityScheduler.runDelayed(() -> {
        }, Duration.ZERO);
        task.cancel();

        Mockito.verify(builder, Mockito.never()).delay(Mockito.any(Duration.class));
        Mockito.verify(scheduledTask).cancel();
    }

    @Test
    void testRepeatingTaskCanBeCancelled() {
        Scheduler scheduler = Mockito.mock(Scheduler.class);
        Scheduler.TaskBuilder builder = Mockito.mock(Scheduler.TaskBuilder.class);
        ScheduledTask scheduledTask = Mockito.mock(ScheduledTask.class);
        Object plugin = new Object();
        Duration interval = Duration.ofSeconds(1);
        Mockito.when(scheduler.buildTask(Mockito.eq(plugin), Mockito.<Consumer<ScheduledTask>>any())).thenReturn(builder);
        Mockito.when(builder.delay(interval)).thenReturn(builder);
        Mockito.when(builder.repeat(interval)).thenReturn(builder);
        Mockito.when(builder.schedule()).thenReturn(scheduledTask);

        var velocityScheduler = new VelocityScheduler(scheduler, plugin);
        var task = velocityScheduler.runAtFixedRate(ignored -> {
        }, interval);
        task.cancel();

        Mockito.verify(builder).delay(interval);
        Mockito.verify(builder).repeat(interval);
        Mockito.verify(scheduledTask).cancel();
    }
}
