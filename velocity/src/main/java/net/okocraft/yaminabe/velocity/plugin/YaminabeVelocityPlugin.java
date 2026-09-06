package net.okocraft.yaminabe.velocity.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.okocraft.yaminabe.common.YaminabeLogger;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.language.LanguageProvider;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
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
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.function.Consumer;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;
import static net.okocraft.yaminabe.common.YaminabeLogger.logDebug;

public final class YaminabeVelocityPlugin {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final YaminabeVelocityConfig.Holder config;
    private @Nullable RestartService restartService;

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

        var scheduler = new VelocityScheduler(this.proxy.getScheduler(), this);
        var shutdownExecutor = new ShutdownExecutor(new VelocityServerController(
            this.proxy,
            () -> VelocityRestartStrategy.from(this.config.get().restart())
        ));
        RestartService restartService = new RestartService(scheduler, new RestartService.Listener() {
            @Override
            public void onExecute(ShutdownReservation reservation) {
                YaminabeVelocityPlugin.this.executeShutdown(shutdownExecutor, reservation);
            }
        });
        this.restartService = restartService;

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
    public void onProxyShutdown(ProxyShutdownEvent event) {
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

    private RestartCommandSettings restartCommandSettings() {
        YaminabeVelocityConfig.Restart restart = this.config.get().restart();
        long countdownSeconds = restart.defaultCountdownSeconds();
        if (countdownSeconds < 0) {
            log().warn("restart.default-countdown-seconds cannot be negative; using 0 instead");
            countdownSeconds = 0;
        }

        ZoneId zoneId = ZoneId.systemDefault();
        String configuredZone = restart.timeZone().strip();
        if (!configuredZone.isEmpty()) {
            try {
                zoneId = ZoneId.of(configuredZone);
            } catch (DateTimeException exception) {
                log().warn("Invalid restart.time-zone '{}'; using system default {}", configuredZone, zoneId, exception);
            }
        }

        return new RestartCommandSettings(Duration.ofSeconds(countdownSeconds), zoneId);
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
