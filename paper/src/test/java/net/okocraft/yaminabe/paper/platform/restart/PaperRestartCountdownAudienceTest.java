package net.okocraft.yaminabe.paper.platform.restart;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

class PaperRestartCountdownAudienceTest {

    @Test
    void testBossBarPresentationUsesGlobalThenEntityScheduler() {
        Fixture fixture = fixture();
        Player player = Mockito.mock(Player.class);
        Mockito.doReturn(List.of(player)).when(fixture.server).getOnlinePlayers();
        BossBar bossBar = bossBar();
        PaperRestartCountdownAudience audience = new PaperRestartCountdownAudience(
            fixture.plugin,
            fixture.entityScheduler
        );

        audience.showBossBar(bossBar);

        Mockito.verify(player, Mockito.never()).showBossBar(bossBar);
        Assertions.assertEquals(1, fixture.globalTasks.size());
        fixture.globalTasks.getFirst().run();
        Mockito.verify(player, Mockito.never()).showBossBar(bossBar);
        Assertions.assertEquals(1, fixture.entityTasks.size());
        fixture.entityTasks.getFirst().run();
        Mockito.verify(player).showBossBar(bossBar);
    }

    @Test
    void testStaleBossBarShowIsDiscardedAfterHide() {
        Fixture fixture = fixture();
        Player player = Mockito.mock(Player.class);
        Mockito.doReturn(List.of(player)).when(fixture.server).getOnlinePlayers();
        BossBar bossBar = bossBar();
        PaperRestartCountdownAudience audience = new PaperRestartCountdownAudience(
            fixture.plugin,
            fixture.entityScheduler
        );

        audience.showBossBar(bossBar);
        fixture.globalTasks.getFirst().run();
        audience.hideBossBar(bossBar);

        fixture.entityTasks.getFirst().run();
        Mockito.verify(player, Mockito.never()).showBossBar(bossBar);
        fixture.globalTasks.get(1).run();
        fixture.entityTasks.get(1).run();
        Mockito.verify(player).hideBossBar(bossBar);
    }

    @Test
    void testBossBarHideDuringDisableDoesNotUsePluginScheduler() {
        Fixture fixture = fixture();
        Player player = Mockito.mock(Player.class);
        Mockito.doReturn(List.of(player)).when(fixture.server).getOnlinePlayers();
        Mockito.when(fixture.plugin.isEnabled()).thenReturn(false);
        BossBar bossBar = bossBar();
        PaperRestartCountdownAudience audience = new PaperRestartCountdownAudience(
            fixture.plugin,
            fixture.entityScheduler
        );

        audience.hideBossBar(bossBar);

        Mockito.verify(player).hideBossBar(bossBar);
        Assertions.assertTrue(fixture.globalTasks.isEmpty());
        Assertions.assertTrue(fixture.entityTasks.isEmpty());
    }

    @Test
    void testMessagePresentationUsesEntityScheduler() {
        Fixture fixture = fixture();
        Player player = Mockito.mock(Player.class);
        Mockito.doReturn(List.of(player)).when(fixture.server).getOnlinePlayers();
        Component message = Component.text("Restarting in 5 seconds");
        PaperRestartCountdownAudience audience = new PaperRestartCountdownAudience(
            fixture.plugin,
            fixture.entityScheduler
        );

        audience.sendMessage(message);
        fixture.globalTasks.getFirst().run();
        fixture.entityTasks.getFirst().run();

        Mockito.verify(player).sendMessage(message);
    }

    private static BossBar bossBar() {
        return BossBar.bossBar(
            Component.text("restart"),
            1.0F,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
        );
    }

    private static Fixture fixture() {
        Plugin plugin = Mockito.mock(Plugin.class);
        Server server = Mockito.mock(Server.class);
        GlobalRegionScheduler globalScheduler = Mockito.mock(GlobalRegionScheduler.class);
        EntityScheduler entityScheduler = Mockito.mock(EntityScheduler.class);
        List<Runnable> globalTasks = new ArrayList<>();
        List<Runnable> entityTasks = new ArrayList<>();
        Mockito.when(plugin.getServer()).thenReturn(server);
        Mockito.when(plugin.isEnabled()).thenReturn(true);
        Mockito.when(server.getGlobalRegionScheduler()).thenReturn(globalScheduler);
        Mockito.doAnswer(invocation -> {
            globalTasks.add(invocation.getArgument(1));
            return null;
        }).when(globalScheduler).execute(Mockito.eq(plugin), Mockito.any(Runnable.class));
        Mockito.when(entityScheduler.execute(Mockito.any(Player.class), Mockito.any(Runnable.class)))
            .thenAnswer(invocation -> {
                entityTasks.add(invocation.getArgument(1));
                return true;
            });
        return new Fixture(plugin, server, entityScheduler, globalTasks, entityTasks);
    }

    private record Fixture(
        Plugin plugin,
        Server server,
        EntityScheduler entityScheduler,
        List<Runnable> globalTasks,
        List<Runnable> entityTasks
    ) {
    }
}
