package net.okocraft.yaminabe.paper.config;

import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;

class PaperRestartSettingsTest {

    @Test
    void testDefaultSettingsMapRestartAndStopSeparately() {
        PaperRestartSettings settings = PaperRestartSettings.from(new YaminabePaperConfig.Restart());

        Assertions.assertEquals(Duration.ofSeconds(60), settings.commandSettings().defaultCountdown());
        Assertions.assertTrue(settings.before(ShutdownType.RESTART).commands().isEmpty());
        Assertions.assertTrue(settings.before(ShutdownType.RESTART).kickPlayers());
        Assertions.assertTrue(settings.before(ShutdownType.STOP).commands().isEmpty());
        Assertions.assertTrue(settings.before(ShutdownType.STOP).kickPlayers());
    }

    @Test
    void testInvalidCommandSettingsAreNormalizedOnLoad(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              default-countdown-seconds: -5
              time-zone: Invalid/Zone
            """);
        var holder = new YaminabePaperConfig.Holder(dir);
        holder.reload();
        var warnings = new ArrayList<String>();

        PaperRestartSettings settings = PaperRestartSettings.from(holder.get().restart(), warnings::add);

        Assertions.assertEquals(Duration.ZERO, settings.commandSettings().defaultCountdown());
        Assertions.assertEquals(ZoneId.systemDefault(), settings.commandSettings().zoneId());
        Assertions.assertEquals(2, warnings.size());
    }
}
