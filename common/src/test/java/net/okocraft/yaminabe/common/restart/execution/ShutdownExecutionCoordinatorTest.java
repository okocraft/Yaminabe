package net.okocraft.yaminabe.common.restart.execution;

import net.okocraft.yaminabe.common.restart.ReservationSource;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

class ShutdownExecutionCoordinatorTest {

    @Test
    void testCountdownCleanupFailureDoesNotPreventShutdownStart() {
        Instant now = Instant.parse("2026-09-06T03:00:00Z");
        ShutdownReservation reservation = ShutdownReservation.create(
            now,
            now,
            Duration.ZERO,
            ShutdownType.RESTART,
            ReservationSource.MANUAL,
            null
        );
        AtomicBoolean shutdownStarted = new AtomicBoolean();

        Assertions.assertDoesNotThrow(() -> ShutdownExecutionCoordinator.start(
            reservation,
            ignored -> {
                throw new IllegalStateException("countdown cleanup failed");
            },
            ignored -> shutdownStarted.set(true)
        ));

        Assertions.assertTrue(shutdownStarted.get());
    }
}
