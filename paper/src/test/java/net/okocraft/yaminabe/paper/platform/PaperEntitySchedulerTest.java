package net.okocraft.yaminabe.paper.platform;

import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

class PaperEntitySchedulerTest {

    private final Plugin plugin = Mockito.mock(Plugin.class);
    private final Server server = Mockito.mock(Server.class);
    private final Entity entity = Mockito.mock(Entity.class);
    private final io.papermc.paper.threadedregions.scheduler.EntityScheduler paperScheduler =
        Mockito.mock(io.papermc.paper.threadedregions.scheduler.EntityScheduler.class);
    private final AtomicInteger runs = new AtomicInteger();

    private EntityScheduler scheduler;

    @BeforeEach
    void setUp() {
        Mockito.when(this.plugin.getServer()).thenReturn(this.server);
        Mockito.when(this.entity.getScheduler()).thenReturn(this.paperScheduler);

        this.scheduler = new PaperEntityScheduler(this.plugin);
    }

    @Test
    void testTaskIsRunRightAwayOnTheOwningThread() {
        Mockito.when(this.server.isOwnedByCurrentRegion(this.entity)).thenReturn(true);

        Assertions.assertTrue(this.scheduler.execute(this.entity, this.runs::incrementAndGet));

        Assertions.assertEquals(1, this.runs.get());
        Mockito.verify(this.entity, Mockito.never()).getScheduler();
    }

    @Test
    void testTaskIsHandedOverOnAnotherThread() {
        Mockito.when(this.server.isOwnedByCurrentRegion(this.entity)).thenReturn(false);
        Mockito.when(this.paperScheduler.execute(
            Mockito.same(this.plugin), Mockito.any(Runnable.class), Mockito.isNull(), Mockito.eq(1L)
        )).thenReturn(true);

        Assertions.assertTrue(this.scheduler.execute(this.entity, this.runs::incrementAndGet));
        Assertions.assertEquals(0, this.runs.get(), "The task is left to the owning thread, so it has not run yet.");

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(this.paperScheduler).execute(
            Mockito.same(this.plugin), task.capture(), Mockito.isNull(), Mockito.eq(1L)
        );

        task.getValue().run();

        Assertions.assertEquals(1, this.runs.get(), "The task the scheduler was handed is the one that was given.");
    }

    @Test
    void testRetiredEntityRejectsTask() {
        Mockito.when(this.server.isOwnedByCurrentRegion(this.entity)).thenReturn(false);
        Mockito.when(this.paperScheduler.execute(
            Mockito.same(this.plugin), Mockito.any(Runnable.class), Mockito.isNull(), Mockito.eq(1L)
        )).thenReturn(false);

        Assertions.assertFalse(this.scheduler.execute(this.entity, this.runs::incrementAndGet));
        Assertions.assertEquals(0, this.runs.get());
    }
}
