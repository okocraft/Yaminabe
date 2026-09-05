package net.okocraft.yaminabe.common.restart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

class RestartDateTimeParserTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    void testParsesTimeOnSameDay() {
        Instant now = Instant.parse("2026-09-06T03:00:00Z"); // 12:00 JST

        Assertions.assertEquals(
            Instant.parse("2026-09-06T09:00:00Z"),
            RestartDateTimeParser.parseFuture("18:00", now, TOKYO)
        );
    }

    @Test
    void testTimeRollsToNextDay() {
        Instant now = Instant.parse("2026-09-06T10:00:00Z"); // 19:00 JST

        Assertions.assertEquals(
            Instant.parse("2026-09-07T09:00:00Z"),
            RestartDateTimeParser.parseFuture("18:00", now, TOKYO)
        );
    }

    @Test
    void testParsesDateTime() {
        Instant now = Instant.parse("2026-09-06T03:00:00Z");

        Assertions.assertEquals(
            Instant.parse("2026-09-10T09:00:00Z"),
            RestartDateTimeParser.parseFuture("2026-09-10T18:00", now, TOKYO)
        );
    }

    @Test
    void testParsesDateTimeWithLowercaseT() {
        Instant now = Instant.parse("2026-09-06T03:00:00Z");

        Assertions.assertEquals(
            Instant.parse("2026-09-10T09:00:00Z"),
            RestartDateTimeParser.parseFuture("2026-09-10t18:00", now, TOKYO)
        );
    }

    @Test
    void testInvalidInputUsesDateOrTimeError() {
        Instant now = Instant.parse("2026-09-06T03:00:00Z");

        IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RestartDateTimeParser.parseFuture("2026-09-10", now, TOKYO)
        );

        Assertions.assertEquals("invalid date/time: 2026-09-10", exception.getMessage());
    }

    @Test
    void testRejectsPastDateTime() {
        Instant now = Instant.parse("2026-09-06T03:00:00Z");

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RestartDateTimeParser.parseFuture("2026-09-05T18:00", now, TOKYO)
        );
    }

    @Test
    void testTimeUsesSecondOccurrenceDuringDstOverlap() {
        Instant now = Instant.parse("2026-11-01T06:00:00Z"); // 01:00 EST, after the first 01:30 EDT

        Assertions.assertEquals(
            Instant.parse("2026-11-01T06:30:00Z"),
            RestartDateTimeParser.parseFuture("01:30", now, NEW_YORK)
        );
    }

    @Test
    void testDateTimeUsesSecondOccurrenceDuringDstOverlap() {
        Instant now = Instant.parse("2026-11-01T06:00:00Z");

        Assertions.assertEquals(
            Instant.parse("2026-11-01T06:30:00Z"),
            RestartDateTimeParser.parseFuture("2026-11-01T01:30", now, NEW_YORK)
        );
    }

    @Test
    void testOverlapUsesEarliestFutureOccurrence() {
        Instant now = Instant.parse("2026-11-01T04:00:00Z");

        Assertions.assertEquals(
            Instant.parse("2026-11-01T05:30:00Z"),
            RestartDateTimeParser.parseFuture("01:30", now, NEW_YORK)
        );
    }

    @Test
    void testDstGapIsShiftedForward() {
        Instant now = Instant.parse("2026-03-08T06:00:00Z"); // 01:00 EST

        Assertions.assertEquals(
            Instant.parse("2026-03-08T07:30:00Z"),
            RestartDateTimeParser.parseFuture("02:30", now, NEW_YORK)
        );
        Assertions.assertEquals(
            Instant.parse("2026-03-08T07:30:00Z"),
            RestartDateTimeParser.parseFuture("2026-03-08T02:30", now, NEW_YORK)
        );
    }
}
