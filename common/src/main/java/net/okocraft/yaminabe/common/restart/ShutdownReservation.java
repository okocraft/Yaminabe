package net.okocraft.yaminabe.common.restart;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record ShutdownReservation(
    Instant createdAt,
    Instant executeAt,
    Instant countdownStartAt,
    ShutdownType type,
    ReservationSource source,
    @Nullable String reason
) {

    public ShutdownReservation {
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(executeAt);
        Objects.requireNonNull(countdownStartAt);
        Objects.requireNonNull(type);
        Objects.requireNonNull(source);

        if (executeAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("executeAt cannot be before createdAt");
        }
        if (countdownStartAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("countdownStartAt cannot be before createdAt");
        }
        if (countdownStartAt.isAfter(executeAt)) {
            throw new IllegalArgumentException("countdownStartAt cannot be after executeAt");
        }
    }

    public static ShutdownReservation create(
        Instant createdAt,
        Instant executeAt,
        Duration countdown,
        ShutdownType type,
        ReservationSource source,
        @Nullable String reason
    ) {
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(executeAt);
        Objects.requireNonNull(countdown);
        if (countdown.isNegative()) {
            throw new IllegalArgumentException("countdown cannot be negative");
        }

        Duration untilExecution = Duration.between(createdAt, executeAt);
        Instant countdownStartAt = countdown.compareTo(untilExecution) >= 0
            ? createdAt
            : executeAt.minus(countdown);
        return new ShutdownReservation(createdAt, executeAt, countdownStartAt, type, source, reason);
    }

    public static ShutdownReservation withFullCountdown(
        Instant createdAt,
        Instant executeAt,
        ShutdownType type,
        ReservationSource source,
        @Nullable String reason
    ) {
        return new ShutdownReservation(createdAt, executeAt, createdAt, type, source, reason);
    }
}
