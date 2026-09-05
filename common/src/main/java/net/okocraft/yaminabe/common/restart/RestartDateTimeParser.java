package net.okocraft.yaminabe.common.restart;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.LongStream;

public final class RestartDateTimeParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .toFormatter();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm[:ss]");

    public static Instant parseFuture(String input, Instant now, ZoneId zoneId) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(now);
        Objects.requireNonNull(zoneId);

        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, DATE_TIME_FORMATTER);
            return resolveFuture(dateTime, now, zoneId)
                .orElseThrow(() -> new IllegalArgumentException("date-time must be in the future: " + input));
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalTime time = LocalTime.parse(input, TIME_FORMATTER);
            LocalDateTime today = LocalDateTime.of(now.atZone(zoneId).toLocalDate(), time);
            return LongStream.rangeClosed(0, 1)
                .mapToObj(today::plusDays)
                .map(dateTime -> resolveFuture(dateTime, now, zoneId))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid date/time: " + input, exception);
        }
    }

    static Optional<Instant> resolveFuture(LocalDateTime dateTime, Instant now, ZoneId zoneId) {
        var validOffsets = zoneId.getRules().getValidOffsets(dateTime);
        if (validOffsets.isEmpty()) {
            Instant candidate = dateTime.atZone(zoneId).toInstant();
            return candidate.isAfter(now) ? Optional.of(candidate) : Optional.empty();
        }
        return validOffsets.stream()
            .map(dateTime::toInstant)
            .filter(candidate -> candidate.isAfter(now))
            .min(Instant::compareTo);
    }

    private RestartDateTimeParser() {
        throw new UnsupportedOperationException();
    }
}
