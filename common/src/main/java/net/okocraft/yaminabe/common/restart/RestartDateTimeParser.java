package net.okocraft.yaminabe.common.restart;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Objects;

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

        if (input.indexOf('T') >= 0) {
            return parseDateTime(input, now, zoneId);
        }
        return parseTime(input, now, zoneId);
    }

    private static Instant parseDateTime(String input, Instant now, ZoneId zoneId) {
        try {
            Instant result = LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(zoneId).toInstant();
            if (!result.isAfter(now)) {
                throw new IllegalArgumentException("date-time must be in the future: " + input);
            }
            return result;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid date-time: " + input, exception);
        }
    }

    private static Instant parseTime(String input, Instant now, ZoneId zoneId) {
        try {
            LocalTime time = LocalTime.parse(input, TIME_FORMATTER);
            ZonedDateTime zonedNow = now.atZone(zoneId);
            ZonedDateTime candidate = ZonedDateTime.of(zonedNow.toLocalDate(), time, zoneId);
            if (!candidate.toInstant().isAfter(now)) {
                candidate = ZonedDateTime.of(zonedNow.toLocalDate().plusDays(1), time, zoneId);
            }
            return candidate.toInstant();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid time: " + input, exception);
        }
    }

    private RestartDateTimeParser() {
        throw new UnsupportedOperationException();
    }
}
