package net.okocraft.yaminabe.paper.config;

import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@NotNullByDefault
public record PaperRestartSettings(
    RestartCommandSettings commandSettings,
    ShutdownSettings beforeRestart,
    ShutdownSettings beforeShutdown
) {

    public PaperRestartSettings {
        Objects.requireNonNull(commandSettings);
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

        long countdownSeconds = restart.defaultCountdownSeconds();
        if (countdownSeconds < 0) {
            warning.accept("restart.default-countdown-seconds cannot be negative; using 0 instead");
            countdownSeconds = 0;
        }

        ZoneId zoneId = ZoneId.systemDefault();
        String configuredZone = restart.timeZone().strip();
        if (!configuredZone.isEmpty()) {
            try {
                zoneId = ZoneId.of(configuredZone);
            } catch (DateTimeException exception) {
                warning.accept("Invalid restart.time-zone '" + configuredZone + "'; using system default " + zoneId);
            }
        }

        return new PaperRestartSettings(
            new RestartCommandSettings(Duration.ofSeconds(countdownSeconds), zoneId),
            ShutdownSettings.from(restart.beforeRestart()),
            ShutdownSettings.from(restart.beforeShutdown())
        );
    }

    public ShutdownSettings before(ShutdownType type) {
        Objects.requireNonNull(type);
        return type == ShutdownType.RESTART ? this.beforeRestart : this.beforeShutdown;
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
