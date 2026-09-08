package net.okocraft.yaminabe.paper.platform.restart;

import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.execution.RestartExecutionMessages;
import net.okocraft.yaminabe.common.restart.execution.ShutdownExecutor;
import net.okocraft.yaminabe.paper.config.PaperRestartSettings;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

@NotNullByDefault
public final class PaperShutdownExecutor {

    private final ShutdownExecutor executor;
    private final Supplier<PaperRestartSettings> settingsSupplier;

    public PaperShutdownExecutor(
        ShutdownExecutor executor,
        Supplier<PaperRestartSettings> settingsSupplier
    ) {
        this.executor = Objects.requireNonNull(executor);
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier);
    }

    public CompletionStage<Void> execute(ShutdownReservation reservation) {
        Objects.requireNonNull(reservation);

        PaperRestartSettings.ShutdownSettings before;
        Component kickReason = null;
        try {
            PaperRestartSettings settings = Objects.requireNonNull(
                this.settingsSupplier.get(),
                "settingsSupplier returned null"
            );
            before = settings.before(reservation.type());
            if (before.kickPlayers()) {
                kickReason = RestartExecutionMessages.kickMessage(reservation).asComponent();
            }
        } catch (RuntimeException | Error failure) {
            log().warn(
                "Failed to prepare Paper shutdown execution; proceeding without pre-shutdown actions",
                failure
            );
            return this.executor.executeFinal(reservation.type());
        }

        if (kickReason == null) {
            return this.executor.executeWithoutKick(reservation.type(), before.commands());
        }
        return this.executor.execute(reservation.type(), before.commands(), kickReason);
    }
}
