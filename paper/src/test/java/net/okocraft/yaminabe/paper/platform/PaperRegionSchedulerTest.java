package net.okocraft.yaminabe.paper.platform;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

class PaperRegionSchedulerTest {

    private final Plugin plugin = Mockito.mock(Plugin.class);
    private final Server server = Mockito.mock(Server.class);
    private final io.papermc.paper.threadedregions.scheduler.RegionScheduler paperScheduler =
        Mockito.mock(io.papermc.paper.threadedregions.scheduler.RegionScheduler.class);
    private final Location location = new Location(Mockito.mock(World.class), 1, 2, 3);
    private final AtomicInteger runs = new AtomicInteger();

    private RegionScheduler scheduler;

    @BeforeEach
    void setUp() {
        Mockito.when(this.plugin.getServer()).thenReturn(this.server);
        Mockito.when(this.server.getRegionScheduler()).thenReturn(this.paperScheduler);

        this.scheduler = new PaperRegionScheduler(this.plugin);
    }

    @Test
    void testTaskIsRunRightAwayOnTheOwningThread() {
        Mockito.when(this.server.isOwnedByCurrentRegion(this.location)).thenReturn(true);

        Assertions.assertTrue(this.scheduler.execute(this.location, this.runs::incrementAndGet));

        Assertions.assertEquals(1, this.runs.get());
        Mockito.verify(this.server, Mockito.never()).getRegionScheduler();
    }

    @Test
    void testTaskIsHandedOverOnAnotherThread() {
        Mockito.when(this.server.isOwnedByCurrentRegion(this.location)).thenReturn(false);

        Assertions.assertFalse(this.scheduler.execute(this.location, this.runs::incrementAndGet));

        Assertions.assertEquals(0, this.runs.get(), "The task is left to the owning thread, so it has not run yet.");

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(this.paperScheduler).execute(Mockito.same(this.plugin), Mockito.same(this.location), task.capture());

        task.getValue().run();

        Assertions.assertEquals(1, this.runs.get(), "The task the scheduler was handed is the one that was given.");
    }
}
