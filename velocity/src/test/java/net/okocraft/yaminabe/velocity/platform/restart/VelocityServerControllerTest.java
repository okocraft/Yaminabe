package net.okocraft.yaminabe.velocity.platform.restart;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;

class VelocityServerControllerTest {

    @Test
    void testDispatchConsoleCommandUsesVelocityConsole() {
        ProxyServer proxy = Mockito.mock(ProxyServer.class);
        CommandManager commandManager = Mockito.mock(CommandManager.class);
        ConsoleCommandSource console = Mockito.mock(ConsoleCommandSource.class);
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);
        Mockito.when(proxy.getCommandManager()).thenReturn(commandManager);
        Mockito.when(proxy.getConsoleCommandSource()).thenReturn(console);
        Mockito.when(commandManager.executeAsync(console, "velocity info")).thenReturn(result);

        var controller = new VelocityServerController(proxy, VelocityRestartStrategy::supervisor);

        Assertions.assertSame(result, controller.dispatchConsoleCommand("velocity info"));
    }

    @Test
    void testKickAllDisconnectsEveryPlayer() {
        ProxyServer proxy = Mockito.mock(ProxyServer.class);
        Player first = Mockito.mock(Player.class);
        Player second = Mockito.mock(Player.class);
        Mockito.when(proxy.getAllPlayers()).thenReturn(List.of(first, second));
        Component reason = Component.text("maintenance");

        var controller = new VelocityServerController(proxy, VelocityRestartStrategy::supervisor);
        controller.kickAll(reason);

        Mockito.verify(first).disconnect(reason);
        Mockito.verify(second).disconnect(reason);
    }

    @Test
    void testRestartPreparesStrategyBeforeShutdown() {
        ProxyServer proxy = Mockito.mock(ProxyServer.class);
        VelocityRestartStrategy strategy = Mockito.mock(VelocityRestartStrategy.class);
        var controller = new VelocityServerController(proxy, () -> strategy);

        controller.restart();

        InOrder order = Mockito.inOrder(strategy, proxy);
        order.verify(strategy).prepare();
        order.verify(proxy).shutdown();
    }

    @Test
    void testRestartStillShutsDownWhenStrategyFails() {
        ProxyServer proxy = Mockito.mock(ProxyServer.class);
        VelocityRestartStrategy strategy = Mockito.mock(VelocityRestartStrategy.class);
        Mockito.doThrow(new IllegalStateException("failed")).when(strategy).prepare();
        var controller = new VelocityServerController(proxy, () -> strategy);

        controller.restart();

        Mockito.verify(proxy).shutdown();
    }

    @Test
    void testStopShutsDownProxy() {
        ProxyServer proxy = Mockito.mock(ProxyServer.class);
        var controller = new VelocityServerController(proxy, VelocityRestartStrategy::supervisor);

        controller.stop();

        Mockito.verify(proxy).shutdown();
    }
}
