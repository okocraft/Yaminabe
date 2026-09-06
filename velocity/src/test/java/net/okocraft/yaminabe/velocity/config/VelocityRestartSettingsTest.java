package net.okocraft.yaminabe.velocity.config;

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
import java.util.List;

class VelocityRestartSettingsTest {

    @Test
    void testEmptyCommandModeFallsBackToSupervisor(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              mode: COMMAND
              command: []
              scheduled:
                enabled: false
            """);
        YaminabeVelocityConfig.Restart restart = load(dir);
        List<String> warnings = new ArrayList<>();

        VelocityRestartSettings settings = VelocityRestartSettings.from(restart, warnings::add);

        Assertions.assertEquals(YaminabeVelocityConfig.RestartMode.SUPERVISOR, settings.mode());
        Assertions.assertTrue(settings.restartCommand().isEmpty());
        Assertions.assertEquals(
            List.of("restart.mode is COMMAND but restart.command is empty; falling back to SUPERVISOR"),
            warnings
        );
    }

    @Test
    void testInvalidValuesAreNormalizedWhenSettingsAreLoaded(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              default-countdown-seconds: -5
              time-zone: Invalid/Zone
              scheduled:
                enabled: true
                times:
                  - invalid
                  - '18:00'
                countdown-seconds: -10
            """);
        YaminabeVelocityConfig.Restart restart = load(dir);
        List<String> warnings = new ArrayList<>();

        VelocityRestartSettings settings = VelocityRestartSettings.from(restart, warnings::add);

        Assertions.assertEquals(Duration.ZERO, settings.commandSettings().defaultCountdown());
        Assertions.assertEquals(ZoneId.systemDefault(), settings.commandSettings().zoneId());
        var automatic = settings.automaticSettings().orElseThrow();
        Assertions.assertEquals(List.of(LocalTime.of(18, 0)), automatic.schedule().times());
        Assertions.assertEquals(Duration.ZERO, automatic.countdown());
        Assertions.assertTrue(warnings.stream().anyMatch(message -> message.contains("restart.default-countdown-seconds")));
        Assertions.assertTrue(warnings.stream().anyMatch(message -> message.contains("Invalid restart.time-zone")));
        Assertions.assertTrue(warnings.stream().anyMatch(message -> message.contains("Invalid automatic restart time 'invalid'")));
        Assertions.assertTrue(warnings.stream().anyMatch(message -> message.contains("restart.scheduled.countdown-seconds")));
    }

    @Test
    void testNoValidScheduledTimesDisablesAutomaticRestart(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              scheduled:
                enabled: true
                times:
                  - invalid
            """);
        List<String> warnings = new ArrayList<>();

        VelocityRestartSettings settings = VelocityRestartSettings.from(load(dir), warnings::add);

        Assertions.assertTrue(settings.automaticSettings().isEmpty());
        Assertions.assertTrue(warnings.stream().anyMatch(message -> message.contains("no valid restart times")));
    }

    @Test
    void testShutdownSettingsAreSelectedByType(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              scheduled:
                enabled: false
              before-restart:
                commands:
                  - restart-command
                kick-players: false
              before-shutdown:
                commands:
                  - stop-command
                kick-players: true
            """);

        VelocityRestartSettings settings = VelocityRestartSettings.from(load(dir), ignored -> {
        });

        Assertions.assertEquals(List.of("restart-command"), settings.before(ShutdownType.RESTART).commands());
        Assertions.assertFalse(settings.before(ShutdownType.RESTART).kickPlayers());
        Assertions.assertEquals(List.of("stop-command"), settings.before(ShutdownType.STOP).commands());
        Assertions.assertTrue(settings.before(ShutdownType.STOP).kickPlayers());
    }

    private static YaminabeVelocityConfig.Restart load(Path dir) throws Exception {
        var holder = new YaminabeVelocityConfig.Holder(dir);
        holder.reload();
        return holder.get().restart();
    }
}
