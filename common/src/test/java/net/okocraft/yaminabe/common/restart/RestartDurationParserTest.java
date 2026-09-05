package net.okocraft.yaminabe.common.restart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class RestartDurationParserTest {

    @Test
    void testParsesBareSeconds() {
        Assertions.assertEquals(Duration.ofSeconds(60), RestartDurationParser.parse("60"));
    }

    @Test
    void testParsesSingleUnit() {
        Assertions.assertEquals(Duration.ofHours(6), RestartDurationParser.parse("6h"));
        Assertions.assertEquals(Duration.ofMinutes(30), RestartDurationParser.parse("30m"));
        Assertions.assertEquals(Duration.ofSeconds(90), RestartDurationParser.parse("90s"));
    }

    @Test
    void testParsesCompoundDuration() {
        Assertions.assertEquals(
            Duration.ofDays(1).plusHours(2).plusMinutes(30).plusSeconds(5),
            RestartDurationParser.parse("1d2h30m5s")
        );
    }

    @Test
    void testRejectsInvalidDuration() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RestartDurationParser.parse("1h 30m"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RestartDurationParser.parse("1h1h"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RestartDurationParser.parse("1m1h"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RestartDurationParser.parse("1x"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> RestartDurationParser.parse(""));
    }
}
