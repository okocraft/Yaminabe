package net.okocraft.yaminabe.common.restart;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Objects;
import java.util.Optional;

public final class RestartDateTimeParser {

    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("HH:mm")
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalEnd()
        .toFormatter();

    public static Instant parseFuture(String input, Instant now, ZoneId zoneId) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(now);
        Objects.requireNonNull(zoneId);

        if (looksLikeDateTime(input)) {
            return parseDateTime(input, now, zoneId);
        }
        return parseTime(input, now, zoneId);
    }

    private static boolean looksLikeDateTime(String input) {
        return input.indexOf('T') >= 0 || input.indexOf('t') >= 0 || input.indexOf('-') >= 0;
    }

    private static Instant parseDateTime(String input, Instant now, ZoneId zoneId) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return resolveFuture(dateTime, now, zoneId)
                .orElseThrow(() -> new IllegalArgumentException("date-time must be in the future: " + input));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid date-time: " + input, exception);
        }
    }

    private static Instant parseTime(String input, Instant now, ZoneId zoneId) {
        try {
            LocalTime time = LocalTime.parse(input, TIME_FORMATTER);
            LocalDateTime candidate = LocalDateTime.of(now.atZone(zoneId).toLocalDate(), time);
            Optional<Instant> sameDay = resolveFuture(candidate, now, zoneId);
            if (sameDay.isPresent()) {
                return sameDay.orElseThrow();
            }
            return resolveFuture(candidate.plusDays(1), now, zoneId).orElseThrow();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid time: " + input, exception);
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
