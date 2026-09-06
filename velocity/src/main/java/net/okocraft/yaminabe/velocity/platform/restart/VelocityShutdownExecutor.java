package net.okocraft.yaminabe.velocity.platform.restart;

import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.execution.RestartExecutionMessages;
import net.okocraft.yaminabe.common.restart.execution.ShutdownExecutor;
import net.okocraft.yaminabe.velocity.config.VelocityRestartSettings;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

@NotNullByDefault
public final class VelocityShutdownExecutor {

    private final ShutdownExecutor executor;
    private final Supplier<VelocityRestartSettings> settingsSupplier;

    public VelocityShutdownExecutor(
        ShutdownExecutor executor,
        Supplier<VelocityRestartSettings> settingsSupplier
    ) {
        this.executor = Objects.requireNonNull(executor);
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier);
    }

    public CompletionStage<Void> execute(ShutdownReservation reservation) {
        Objects.requireNonNull(reservation);
        VelocityRestartSettings.ShutdownSettings before = this.settings().before(reservation.type());
        if (!before.kickPlayers()) {
            return this.executor.executeWithoutKick(reservation.type(), before.commands());
        }
        return this.executor.execute(
            reservation.type(),
            before.commands(),
            RestartExecutionMessages.kickMessage(reservation).asComponent()
        );
    }

    private VelocityRestartSettings settings() {
        return Objects.requireNonNull(this.settingsSupplier.get(), "settingsSupplier returned null");
    }
}
