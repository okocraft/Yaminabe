package net.okocraft.yaminabe.common.restart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

class RestartScheduleTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

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
    void testEmptyScheduleHasNoNextTime() {
        RestartSchedule schedule = new RestartSchedule(TOKYO, List.of());
        Assertions.assertTrue(schedule.nextAfter(Instant.EPOCH).isEmpty());
    }
}
