package net.okocraft.yaminabe.velocity.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.okocraft.yaminabe.common.YaminabeLogger;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.language.LanguageProvider;
import net.okocraft.yaminabe.common.restart.AutomaticRestartManager;
import net.okocraft.yaminabe.common.restart.RestartSchedule;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownPresenter;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownSettings;
import net.okocraft.yaminabe.common.restart.execution.RestartExecutionMessages;
import net.okocraft.yaminabe.common.restart.execution.ShutdownExecutor;
import net.okocraft.yaminabe.velocity.command.YaminabeCommands;
import net.okocraft.yaminabe.velocity.config.YaminabeVelocityConfig;
import net.okocraft.yaminabe.velocity.platform.VelocityScheduler;
import net.okocraft.yaminabe.velocity.platform.restart.VelocityRestartStrategy;
import net.okocraft.yaminabe.velocity.platform.restart.VelocityServerController;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.SubstituteLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;
import static net.okocraft.yaminabe.common.YaminabeLogger.logDebug;

public final class YaminabeVelocityPlugin {

    private static final DateTimeFormatter SCHEDULED_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
        .toFormatter(Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT);

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final YaminabeVelocityConfig.Holder config;
    private @Nullable RestartService restartService;
    private @Nullable AutomaticRestartManager automaticRestartManager;
    private @Nullable RestartCountdownPresenter restartCountdownPresenter;

    @Inject
    public YaminabeVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        ((SubstituteLogger) YaminabeLogger.log()).setDelegate(logger);
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.config = new YaminabeVelocityConfig.Holder(dataDirectory);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            this.loadConfig();
        } catch (IOException e) {
            log().error("Failed to load the config file", e);
            return;
        }

        try {
            this.loadLanguages();
        } catch (IOException e) {
            log().error("Failed to load language files", e);
        }

        Clock clock = Clock.systemUTC();
        var scheduler = new VelocityScheduler(this.proxy.getScheduler(), this);
        var shutdownExecutor = new ShutdownExecutor(new VelocityServerController(
            this.proxy,
            () -> VelocityRestartStrategy.from(this.config.get().restart())
        ));
        RestartCountdownPresenter countdownPresenter = new RestartCountdownPresenter(
            scheduler,
            clock,
            this.proxy::getAllPlayers,
            this::restartCountdownSettings
        );
        this.restartCountdownPresenter = countdownPresenter;

        RestartService restartService = new RestartService(scheduler, clock, new RestartService.Listener() {
            @Override
            public void onCountdownStarted(ShutdownReservation reservation) {
                countdownPresenter.start(reservation);
            }

            @Override
            public void onCancelled(ShutdownReservation reservation) {
                countdownPresenter.stop(reservation);
                AutomaticRestartManager manager = YaminabeVelocityPlugin.this.automaticRestartManager;
                if (manager != null) {
                    manager.onCancelled(reservation);
                }
            }

            @Override
            public void onExecute(ShutdownReservation reservation) {
                countdownPresenter.stop(reservation);
                YaminabeVelocityPlugin.this.executeShutdown(shutdownExecutor, reservation);
            }
        });
        this.restartService = restartService;

        AutomaticRestartManager automaticRestartManager = new AutomaticRestartManager(
            restartService,
            clock,
            this::automaticRestartSettings
        );
        this.automaticRestartManager = automaticRestartManager;
        automaticRestartManager.refresh();

        YaminabeCommands.register(
            this.proxy.getCommandManager(),
            this,
            scheduler,
            this::reload,
            restartService,
            this::restartCommandSettings
        );
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        RestartCountdownPresenter presenter = this.restartCountdownPresenter;
        if (presenter != null) {
            presenter.showTo(event.getPlayer());
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        AutomaticRestartManager manager = this.automaticRestartManager;
        if (manager != null) {
            manager.close();
            this.automaticRestartManager = null;
        }

        RestartCountdownPresenter presenter = this.restartCountdownPresenter;
        if (presenter != null) {
            presenter.close();
            this.restartCountdownPresenter = null;
        }

        RestartService service = this.restartService;
        if (service != null) {
            service.close();
            this.restartService = null;
        }
        LanguageProvider.unload();
    }

    private void reload(Consumer<YaminabeReloader.Notification> consumer) {
        try {
            this.loadConfig();
            AutomaticRestartManager manager = this.automaticRestartManager;
            if (manager != null) {
                manager.refresh();
            }
            consumer.accept(YaminabeReloader.Notification.CONFIG_RELOADED);
        } catch (IOException e) {
            log().error("Failed to reload config", e);
            consumer.accept(YaminabeReloader.Notification.FAILED_TO_RELOAD_CONFIG);
        }

        try {
            LanguageProvider.unload();
            this.loadLanguages();
            consumer.accept(YaminabeReloader.Notification.LANGUAGE_RELOADED);
        } catch (IOException e) {
            log().error("Failed to reload languages", e);
            consumer.accept(YaminabeReloader.Notification.FAILED_TO_RELOAD_LANGUAGES);
        }
    }

    private void executeShutdown(ShutdownExecutor executor, ShutdownReservation reservation) {
        YaminabeVelocityConfig.Restart restart = this.config.get().restart();
        YaminabeVelocityConfig.BeforeShutdown before = reservation.type() == ShutdownType.RESTART
            ? restart.beforeRestart()
            : restart.beforeShutdown();

        var execution = before.kickPlayers()
            ? executor.execute(
                reservation.type(),
                before.commands(),
                RestartExecutionMessages.kickMessage(reservation).asComponent()
            )
            : executor.execute(reservation.type(), before.commands());

        execution.whenComplete((ignored, failure) -> {
            if (failure != null) {
                log().error("Velocity shutdown execution completed exceptionally", failure);
            }
        });
    }

    private Optional<AutomaticRestartManager.Settings> automaticRestartSettings() {
        YaminabeVelocityConfig.Restart restart = this.config.get().restart();
        YaminabeVelocityConfig.Scheduled scheduled = restart.scheduled();
        if (!scheduled.enabled()) {
            return Optional.empty();
        }

        ZoneId zoneId = this.restartZoneId(restart);
        var times = new ArrayList<LocalTime>();
        for (String input : scheduled.times()) {
            try {
                times.add(LocalTime.parse(input.strip(), SCHEDULED_TIME_FORMATTER));
            } catch (DateTimeParseException exception) {
                log().warn("Invalid automatic restart time '{}'; skipping it", input);
            }
        }
        if (times.isEmpty()) {
            log().info("Automatic restart is disabled because no valid restart times are configured");
            return Optional.empty();
        }

        long countdownSeconds = scheduled.countdownSeconds();
        if (countdownSeconds < 0) {
            log().warn("restart.scheduled.countdown-seconds cannot be negative; using 0 instead");
            countdownSeconds = 0;
        }
        return Optional.of(new AutomaticRestartManager.Settings(
            new RestartSchedule(zoneId, times),
            Duration.ofSeconds(countdownSeconds)
        ));
    }

    private RestartCountdownSettings restartCountdownSettings() {
        YaminabeVelocityConfig.Countdown countdown = this.config.get().restart().countdown();
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

    private RestartCommandSettings restartCommandSettings() {
        YaminabeVelocityConfig.Restart restart = this.config.get().restart();
        long countdownSeconds = restart.defaultCountdownSeconds();
        if (countdownSeconds < 0) {
            log().warn("restart.default-countdown-seconds cannot be negative; using 0 instead");
            countdownSeconds = 0;
        }
        return new RestartCommandSettings(Duration.ofSeconds(countdownSeconds), this.restartZoneId(restart));
    }

    private ZoneId restartZoneId(YaminabeVelocityConfig.Restart restart) {
        ZoneId zoneId = ZoneId.systemDefault();
        String configuredZone = restart.timeZone().strip();
        if (!configuredZone.isEmpty()) {
            try {
                zoneId = ZoneId.of(configuredZone);
            } catch (DateTimeException exception) {
                log().warn("Invalid restart.time-zone '{}'; using system default {}", configuredZone, zoneId, exception);
            }
        }
        return zoneId;
    }

    private void loadConfig() throws IOException {
        this.config.reload();

        boolean debug = this.config.get().debug();
        logDebug(debug);

        if (debug) {
            log().info("Debug mode enabled");
        }
    }

    private void loadLanguages() throws IOException {
        LanguageProvider.load(this.dataDirectory.resolve("languages"), YaminabeCommands.getDefiners());
    }
}
