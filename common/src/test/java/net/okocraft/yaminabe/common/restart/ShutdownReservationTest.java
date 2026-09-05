package net.okocraft.yaminabe.common.restart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

class ShutdownReservationTest {

    @Test
    void testCountdownStartIsCalculatedFromExecutionTime() {
        Instant createdAt = Instant.parse("2026-09-06T00:00:00Z");
        Instant executeAt = createdAt.plus(Duration.ofHours(6));

        ShutdownReservation reservation = ShutdownReservation.create(
            createdAt,
            executeAt,
            Duration.ofMinutes(5),
            ShutdownType.RESTART,
            ReservationSource.MANUAL,
            "maintenance"
        );

        Assertions.assertEquals(executeAt.minus(Duration.ofMinutes(5)), reservation.countdownStartAt());
    }

    @Test
    void testCountdownLongerThanReservationIsClampedToCreationTime() {
        Instant createdAt = Instant.parse("2026-09-06T00:00:00Z");
        Instant executeAt = createdAt.plus(Duration.ofMinutes(10));

        ShutdownReservation reservation = ShutdownReservation.create(
            createdAt,
            executeAt,
            Duration.ofMinutes(30),
            ShutdownType.RESTART,
            ReservationSource.MANUAL,
            null
        );

        Assertions.assertEquals(createdAt, reservation.countdownStartAt());
    }

    @Test
    void testFullCountdownStartsAtCreationTime() {
        Instant createdAt = Instant.parse("2026-09-06T00:00:00Z");
        Instant executeAt = createdAt.plus(Duration.ofHours(1));

        ShutdownReservation reservation = ShutdownReservation.withFullCountdown(
            createdAt,
            executeAt,
            ShutdownType.STOP,
            ReservationSource.MANUAL,
            null
        );

        Assertions.assertEquals(createdAt, reservation.countdownStartAt());
    }
}
