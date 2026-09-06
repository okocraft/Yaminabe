package net.okocraft.yaminabe.velocity.platform.restart;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.execution.ServerController;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

@NotNullByDefault
public final class VelocityServerController implements ServerController {

    private final ProxyServer proxy;
    private final Supplier<VelocityRestartStrategy> restartStrategySupplier;

    public VelocityServerController(
        ProxyServer proxy,
        Supplier<VelocityRestartStrategy> restartStrategySupplier
    ) {
        this.proxy = Objects.requireNonNull(proxy);
        this.restartStrategySupplier = Objects.requireNonNull(restartStrategySupplier);
    }

    @Override
    public CompletionStage<Boolean> dispatchConsoleCommand(String command) {
        return this.proxy.getCommandManager().executeAsync(this.proxy.getConsoleCommandSource(), command);
    }

    @Override
    public void kickAll(Component reason) {
        for (var player : this.proxy.getAllPlayers()) {
            try {
                player.disconnect(reason);
            } catch (RuntimeException exception) {
                log().warn("Failed to disconnect player {} before Velocity shutdown", player.getUsername(), exception);
            }
        }
    }

    @Override
    public void stop() {
        this.proxy.shutdown();
    }

    @Override
    public void restart() {
        try {
            Objects.requireNonNull(this.restartStrategySupplier.get(), "restartStrategySupplier returned null").prepare();
        } catch (RuntimeException exception) {
            log().error("Failed to prepare the Velocity restart strategy; the proxy will still shut down", exception);
        } finally {
            this.proxy.shutdown();
        }
    }
}
