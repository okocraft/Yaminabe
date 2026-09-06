package net.okocraft.yaminabe.paper.config;

import net.okocraft.yaminabe.common.restart.AutomaticRestartManager;
import net.okocraft.yaminabe.common.restart.RestartSchedule;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownSettings;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@NotNullByDefault
public record PaperRestartSettings(
    RestartCommandSettings commandSettings,
    Optional<AutomaticRestartManager.Settings> automaticSettings,
    RestartCountdownSettings countdownSettings,
    ShutdownSettings beforeRestart,
    ShutdownSettings beforeShutdown
) {

    private static final DateTimeFormatter SCHEDULED_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
        .toFormatter(Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT);

    public PaperRestartSettings {
        Objects.requireNonNull(commandSettings);
        Objects.requireNonNull(automaticSettings);
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

        ZoneId zoneId = parseZoneId(restart.timeZone(), warning);
        long countdownSeconds = nonNegative(
            restart.defaultCountdownSeconds(),
            "restart.default-countdown-seconds",
            warning
        );

        return new PaperRestartSettings(
            new RestartCommandSettings(Duration.ofSeconds(countdownSeconds), zoneId),
            automaticSettings(restart.scheduled(), zoneId, warning),
            countdownSettings(restart.countdown()),
            ShutdownSettings.from(restart.beforeRestart()),
            ShutdownSettings.from(restart.beforeShutdown())
        );
    }

    public ShutdownSettings before(ShutdownType type) {
        Objects.requireNonNull(type);
        return type == ShutdownType.RESTART ? this.beforeRestart : this.beforeShutdown;
    }

    private static Optional<AutomaticRestartManager.Settings> automaticSettings(
        YaminabePaperConfig.Scheduled scheduled,
        ZoneId zoneId,
        Consumer<String> warning
    ) {
        if (!scheduled.enabled()) {
            return Optional.empty();
        }

        var times = new ArrayList<LocalTime>();
        for (String input : scheduled.times()) {
            try {
                times.add(LocalTime.parse(input.strip(), SCHEDULED_TIME_FORMATTER));
            } catch (DateTimeParseException exception) {
                warning.accept("Invalid automatic restart time '" + input + "'; skipping it");
            }
        }
        if (times.isEmpty()) {
            warning.accept("Automatic restart is disabled because no valid restart times are configured");
            return Optional.empty();
        }

        long countdownSeconds = nonNegative(
            scheduled.countdownSeconds(),
            "restart.scheduled.countdown-seconds",
            warning
        );
        return Optional.of(new AutomaticRestartManager.Settings(
            new RestartSchedule(zoneId, times),
            Duration.ofSeconds(countdownSeconds)
        ));
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
