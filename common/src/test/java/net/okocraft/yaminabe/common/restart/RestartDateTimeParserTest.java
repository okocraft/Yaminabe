package net.okocraft.yaminabe.common.restart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

class RestartDateTimeParserTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

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
    void testRejectsPastDateTime() {
        Instant now = Instant.parse("2026-09-06T03:00:00Z");

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RestartDateTimeParser.parseFuture("2026-09-05T18:00", now, TOKYO)
        );
    }
}
