package net.okocraft.yaminabe.common.restart.execution;

import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.function.Consumer;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

@NotNullByDefault
public final class ShutdownExecutionCoordinator {

    private ShutdownExecutionCoordinator() {
    }

    public static void start(
        ShutdownReservation reservation,
        Consumer<ShutdownReservation> countdownCleanup,
        Consumer<ShutdownReservation> shutdownStarter
    ) {
        Objects.requireNonNull(reservation);
        Objects.requireNonNull(countdownCleanup);
        Objects.requireNonNull(shutdownStarter);

        try {
            countdownCleanup.accept(reservation);
        } catch (RuntimeException | Error failure) {
            log().warn("Failed to clean up the restart countdown before shutdown execution", failure);
        }

        shutdownStarter.accept(reservation);
    }
}
