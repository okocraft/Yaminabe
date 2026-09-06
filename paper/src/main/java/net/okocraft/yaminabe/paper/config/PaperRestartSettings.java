package net.okocraft.yaminabe.paper.config;

import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownSettings;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@NotNullByDefault
public record PaperRestartSettings(
    RestartCommandSettings commandSettings,
    RestartCountdownSettings countdownSettings,
    ShutdownSettings beforeRestart,
    ShutdownSettings beforeShutdown
) {

    public PaperRestartSettings {
        Objects.requireNonNull(commandSettings);
        Objects.requireNonNull(countdownSettings);
        Objects.requireNonNull(beforeRestart);
        Objects.requireNonNull(beforeShutdown);
    }

    public static PaperRestartSettings from(YaminabePaperConfig.Restart restart) {
        return from(restart, ignored -> {
        });
    }

    public static PaperRestartSettings from(
        YaminabePaperConfig.Restart restart,
        Consumer<String> warning
    ) {
        Objects.requireNonNull(restart);
        Objects.requireNonNull(warning);

        long countdownSeconds = nonNegative(
            restart.defaultCountdownSeconds(),
            "restart.default-countdown-seconds",
            warning
        );
        ZoneId zoneId = parseZoneId(restart.timeZone(), warning);

        return new PaperRestartSettings(
            new RestartCommandSettings(Duration.ofSeconds(countdownSeconds), zoneId),
            countdownSettings(restart.countdown()),
            ShutdownSettings.from(restart.beforeRestart()),
            ShutdownSettings.from(restart.beforeShutdown())
        );
    }

    public ShutdownSettings before(ShutdownType type) {
        Objects.requireNonNull(type);
        return type == ShutdownType.RESTART ? this.beforeRestart : this.beforeShutdown;
    }

    private static RestartCountdownSettings countdownSettings(YaminabePaperConfig.Countdown countdown) {
        Set<Long> broadcastAtSeconds = new HashSet<>();
        for (int seconds : countdown.broadcastAtSeconds()) {
            if (seconds > 0) {
                broadcastAtSeconds.add((long) seconds);
            }
        }
        return new RestartCountdownSettings(
            countdown.bossBar().enabled(),
            countdown.bossBar().color(),
            countdown.bossBar().overlay(),
            broadcastAtSeconds
        );
    }

    private static ZoneId parseZoneId(String input, Consumer<String> warning) {
        ZoneId systemDefault = ZoneId.systemDefault();
        String configuredZone = input.strip();
        if (configuredZone.isEmpty()) {
            return systemDefault;
        }
        try {
            return ZoneId.of(configuredZone);
        } catch (DateTimeException exception) {
            warning.accept("Invalid restart.time-zone '" + configuredZone + "'; using system default " + systemDefault);
            return systemDefault;
        }
    }

    private static long nonNegative(long value, String path, Consumer<String> warning) {
        if (value >= 0) {
            return value;
        }
        warning.accept(path + " cannot be negative; using 0 instead");
        return 0;
    }

    public record ShutdownSettings(List<String> commands, boolean kickPlayers) {

        public ShutdownSettings {
            commands = List.copyOf(commands);
        }

        private static ShutdownSettings from(YaminabePaperConfig.BeforeShutdown settings) {
            return new ShutdownSettings(settings.commands(), settings.kickPlayers());
        }
    }
}
