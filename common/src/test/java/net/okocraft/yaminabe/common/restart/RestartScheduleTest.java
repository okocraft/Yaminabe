package net.okocraft.yaminabe.common.restart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

class RestartScheduleTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    void testFindsNextTimeOnSameDay() {
        RestartSchedule schedule = new RestartSchedule(TOKYO, List.of(LocalTime.of(6, 0), LocalTime.of(18, 0)));
        Instant now = Instant.parse("2026-09-06T03:00:00Z"); // 12:00 JST

        Assertions.assertEquals(
            Instant.parse("2026-09-06T09:00:00Z"),
            schedule.nextAfter(now).orElseThrow()
        );
    }

    @Test
    void testRollsToNextDay() {
        RestartSchedule schedule = new RestartSchedule(TOKYO, List.of(LocalTime.of(6, 0), LocalTime.of(18, 0)));
        Instant now = Instant.parse("2026-09-06T10:00:00Z"); // 19:00 JST

        Assertions.assertEquals(
            Instant.parse("2026-09-06T21:00:00Z"),
            schedule.nextAfter(now).orElseThrow()
        );
    }

    @Test
    void testDstGapUsesEarliestResolvedInstant() {
        RestartSchedule schedule = new RestartSchedule(
            NEW_YORK,
            List.of(LocalTime.of(2, 30), LocalTime.of(3, 0))
        );
        Instant now = Instant.parse("2026-03-08T06:00:00Z"); // 01:00 EST

        Assertions.assertEquals(
            Instant.parse("2026-03-08T07:00:00Z"),
            schedule.nextAfter(now).orElseThrow()
        );
    }

    @Test
    void testDstOverlapUsesSecondOccurrenceWhenFirstIsPast() {
        RestartSchedule schedule = new RestartSchedule(NEW_YORK, List.of(LocalTime.of(1, 30)));
        Instant now = Instant.parse("2026-11-01T06:00:00Z"); // 01:00 EST, after 01:30 EDT

        Assertions.assertEquals(
            Instant.parse("2026-11-01T06:30:00Z"),
            schedule.nextAfter(now).orElseThrow()
        );
    }

    @Test
    void testEmptyScheduleHasNoNextTime() {
        RestartSchedule schedule = new RestartSchedule(TOKYO, List.of());
        Assertions.assertTrue(schedule.nextAfter(Instant.EPOCH).isEmpty());
    }
}
