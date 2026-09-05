package net.okocraft.yaminabe.paper.platform;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

class PaperAsyncSchedulerTest {

    @Test
    void testDelayedTaskCanBeCancelled() {
        Plugin plugin = Mockito.mock(Plugin.class);
        Server server = Mockito.mock(Server.class);
        AsyncScheduler asyncScheduler = Mockito.mock(AsyncScheduler.class);
        ScheduledTask scheduledTask = Mockito.mock(ScheduledTask.class);
        Mockito.when(plugin.getServer()).thenReturn(server);
        Mockito.when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
        Mockito.when(asyncScheduler.runDelayed(Mockito.eq(plugin), Mockito.any(), Mockito.eq(1000L), Mockito.eq(TimeUnit.MILLISECONDS)))
            .thenReturn(scheduledTask);

        var scheduler = new PaperAsyncScheduler(plugin);
        var task = scheduler.runDelayed(() -> {
        }, Duration.ofSeconds(1));
        task.cancel();

        Mockito.verify(scheduledTask).cancel();
    }

    @Test
    void testZeroDelayTaskCanBeCancelled() {
        Plugin plugin = Mockito.mock(Plugin.class);
        Server server = Mockito.mock(Server.class);
        AsyncScheduler asyncScheduler = Mockito.mock(AsyncScheduler.class);
        ScheduledTask scheduledTask = Mockito.mock(ScheduledTask.class);
        Mockito.when(plugin.getServer()).thenReturn(server);
        Mockito.when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
        Mockito.when(asyncScheduler.runNow(Mockito.eq(plugin), Mockito.any())).thenReturn(scheduledTask);

        var scheduler = new PaperAsyncScheduler(plugin);
        var task = scheduler.runDelayed(() -> {
        }, Duration.ZERO);
        task.cancel();

        Mockito.verify(asyncScheduler).runNow(Mockito.eq(plugin), Mockito.any());
        Mockito.verify(scheduledTask).cancel();
    }

    @Test
    void testRepeatingTaskCanBeCancelled() {
        Plugin plugin = Mockito.mock(Plugin.class);
        Server server = Mockito.mock(Server.class);
        AsyncScheduler asyncScheduler = Mockito.mock(AsyncScheduler.class);
        ScheduledTask scheduledTask = Mockito.mock(ScheduledTask.class);
        Mockito.when(plugin.getServer()).thenReturn(server);
        Mockito.when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
        Mockito.when(asyncScheduler.runAtFixedRate(
            Mockito.eq(plugin), Mockito.any(), Mockito.eq(1000L), Mockito.eq(1000L), Mockito.eq(TimeUnit.MILLISECONDS)
        )).thenReturn(scheduledTask);

        var scheduler = new PaperAsyncScheduler(plugin);
        var task = scheduler.runAtFixedRate(ignored -> {
        }, Duration.ofSeconds(1));
        task.cancel();

        Mockito.verify(scheduledTask).cancel();
    }
}
