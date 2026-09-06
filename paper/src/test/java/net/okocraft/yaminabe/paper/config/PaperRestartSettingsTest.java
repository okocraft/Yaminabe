package net.okocraft.yaminabe.paper.config;

import net.kyori.adventure.bossbar.BossBar;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Set;

class PaperRestartSettingsTest {

    @Test
    void testDefaultSettingsKeepAutomaticRestartDisabled() {
        PaperRestartSettings settings = PaperRestartSettings.from(new YaminabePaperConfig.Restart());

        Assertions.assertEquals(Duration.ofSeconds(60), settings.commandSettings().defaultCountdown());
        Assertions.assertTrue(settings.automaticSettings().isEmpty());
        Assertions.assertTrue(settings.countdownSettings().bossBarEnabled());
        Assertions.assertEquals(BossBar.Color.RED, settings.countdownSettings().bossBarColor());
        Assertions.assertEquals(BossBar.Overlay.NOTCHED_10, settings.countdownSettings().bossBarOverlay());
        Assertions.assertEquals(Set.of(60L, 30L, 10L, 5L, 4L, 3L, 2L, 1L), settings.countdownSettings().broadcastAtSeconds());
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

    @Test
    void testEnabledAutomaticSettingsUseConfiguredSchedule(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              time-zone: Asia/Tokyo
              scheduled:
                enabled: true
                times:
                  - '06:00'
                countdown-seconds: 60
            """);
        var holder = new YaminabePaperConfig.Holder(dir);
        holder.reload();

        PaperRestartSettings settings = PaperRestartSettings.from(holder.get().restart());
        var automatic = settings.automaticSettings().orElseThrow();

        Assertions.assertEquals(ZoneId.of("Asia/Tokyo"), automatic.schedule().zoneId());
        Assertions.assertEquals(java.util.List.of(LocalTime.of(6, 0)), automatic.schedule().times());
        Assertions.assertEquals(Duration.ofSeconds(60), automatic.countdown());
    }

    @Test
    void testInvalidAutomaticTimesAreSkipped(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              time-zone: Asia/Tokyo
              scheduled:
                enabled: true
                times:
                  - invalid
                  - '18:30'
                countdown-seconds: -10
            """);
        var holder = new YaminabePaperConfig.Holder(dir);
        holder.reload();
        var warnings = new ArrayList<String>();

        PaperRestartSettings settings = PaperRestartSettings.from(holder.get().restart(), warnings::add);
        var automatic = settings.automaticSettings().orElseThrow();

        Assertions.assertEquals(ZoneId.of("Asia/Tokyo"), automatic.schedule().zoneId());
        Assertions.assertEquals(java.util.List.of(LocalTime.of(18, 30)), automatic.schedule().times());
        Assertions.assertEquals(Duration.ZERO, automatic.countdown());
        Assertions.assertEquals(2, warnings.size());
    }

    @Test
    void testNoValidAutomaticTimesDisablesAutomaticRestart(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              scheduled:
                enabled: true
                times:
                  - invalid
            """);
        var holder = new YaminabePaperConfig.Holder(dir);
        holder.reload();
        var warnings = new ArrayList<String>();

        PaperRestartSettings settings = PaperRestartSettings.from(holder.get().restart(), warnings::add);

        Assertions.assertTrue(settings.automaticSettings().isEmpty());
        Assertions.assertEquals(2, warnings.size());
    }
}
