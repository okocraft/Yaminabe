package net.okocraft.yaminabe.paper.platform.restart;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class PaperServerControllerTest {

    @Test
    void testDispatchConsoleCommandRunsOnGlobalRegion() {
        Fixture fixture = fixture(true);
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Mockito.when(fixture.server.getConsoleSender()).thenReturn(console);
        Mockito.when(fixture.server.dispatchCommand(console, "save-all")).thenReturn(true);

        var controller = new PaperServerController(fixture.plugin, fixture.entityScheduler);

        Assertions.assertTrue(controller.dispatchConsoleCommand("save-all").toCompletableFuture().join());
        Mockito.verify(fixture.server).dispatchCommand(console, "save-all");
    }

    @Test
    void testKickAllWaitsForEveryEntityTask() {
        Fixture fixture = fixture(true);
        Player first = Mockito.mock(Player.class);
        Player second = Mockito.mock(Player.class);
        Mockito.doReturn(List.of(first, second)).when(fixture.server).getOnlinePlayers();
        AtomicReference<Runnable> firstTask = new AtomicReference<>();
        AtomicReference<Runnable> secondTask = new AtomicReference<>();
        Mockito.when(fixture.entityScheduler.execute(Mockito.eq(first), Mockito.any(Runnable.class)))
            .thenAnswer(invocation -> {
                firstTask.set(invocation.getArgument(1));
                return true;
            });
        Mockito.when(fixture.entityScheduler.execute(Mockito.eq(second), Mockito.any(Runnable.class)))
            .thenAnswer(invocation -> {
                secondTask.set(invocation.getArgument(1));
                return true;
            });
        Component reason = Component.text("maintenance");

        var controller = new PaperServerController(fixture.plugin, fixture.entityScheduler);
        var result = controller.kickAll(reason).toCompletableFuture();

        Assertions.assertFalse(result.isDone());
        firstTask.get().run();
        Assertions.assertFalse(result.isDone());
        secondTask.get().run();
        Assertions.assertTrue(result.isDone());
        Mockito.verify(first).kick(reason);
        Mockito.verify(second).kick(reason);
    }

    @Test
    void testRetiredPlayerDoesNotBlockKickCompletion() {
        Fixture fixture = fixture(true);
        Player player = Mockito.mock(Player.class);
        Mockito.doReturn(List.of(player)).when(fixture.server).getOnlinePlayers();
        Mockito.when(fixture.entityScheduler.execute(Mockito.eq(player), Mockito.any(Runnable.class))).thenReturn(false);

        var controller = new PaperServerController(fixture.plugin, fixture.entityScheduler);

        Assertions.assertDoesNotThrow(() -> controller.kickAll(Component.empty()).toCompletableFuture().join());
        Mockito.verify(player, Mockito.never()).kick(Mockito.any(Component.class));
    }

    @Test
    void testRestartAndStopAreScheduledOnGlobalRegion() {
        Fixture fixture = fixture(false);
        List<Runnable> globalTasks = new ArrayList<>();
        Mockito.doAnswer(invocation -> {
            globalTasks.add(invocation.getArgument(1));
            return null;
        }).when(fixture.globalScheduler).execute(Mockito.eq(fixture.plugin), Mockito.any(Runnable.class));
        var controller = new PaperServerController(fixture.plugin, fixture.entityScheduler);

        controller.restart();
        controller.stop();

        Mockito.verify(fixture.server, Mockito.never()).restart();
        Mockito.verify(fixture.server, Mockito.never()).shutdown();
        Assertions.assertEquals(2, globalTasks.size());
        globalTasks.get(0).run();
        globalTasks.get(1).run();
        Mockito.verify(fixture.server).restart();
        Mockito.verify(fixture.server).shutdown();
    }

    private static Fixture fixture(boolean runGlobalImmediately) {
        Plugin plugin = Mockito.mock(Plugin.class);
        Server server = Mockito.mock(Server.class);
        GlobalRegionScheduler globalScheduler = Mockito.mock(GlobalRegionScheduler.class);
        EntityScheduler entityScheduler = Mockito.mock(EntityScheduler.class);
        Mockito.when(plugin.getServer()).thenReturn(server);
        Mockito.when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
        if (runGlobalImmediately) {
            Mockito.doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(1)).run();
                return null;
            }).when(globalScheduler).execute(Mockito.eq(plugin), Mockito.any(Runnable.class));
        }
        return new Fixture(plugin, server, globalScheduler, entityScheduler);
    }

    private record Fixture(
        Plugin plugin,
        Server server,
        GlobalRegionScheduler globalScheduler,
        EntityScheduler entityScheduler
    ) {
    }
}
