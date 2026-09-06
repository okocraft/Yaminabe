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
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownPresenter;
import net.okocraft.yaminabe.common.restart.execution.ShutdownExecutor;
import net.okocraft.yaminabe.velocity.command.YaminabeCommands;
import net.okocraft.yaminabe.velocity.config.VelocityRestartSettings;
import net.okocraft.yaminabe.velocity.config.YaminabeVelocityConfig;
import net.okocraft.yaminabe.velocity.platform.VelocityScheduler;
import net.okocraft.yaminabe.velocity.platform.restart.VelocityRestartStrategy;
import net.okocraft.yaminabe.velocity.platform.restart.VelocityServerController;
import net.okocraft.yaminabe.velocity.platform.restart.VelocityShutdownExecutor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.SubstituteLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;
import static net.okocraft.yaminabe.common.YaminabeLogger.logDebug;

public final class YaminabeVelocityPlugin {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final YaminabeVelocityConfig.Holder config;
    private volatile @Nullable VelocityRestartSettings restartSettings;
    private volatile @Nullable RestartService restartService;
    private volatile @Nullable AutomaticRestartManager automaticRestartManager;
    private volatile @Nullable RestartCountdownPresenter restartCountdownPresenter;

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
        var shutdownExecutor = new VelocityShutdownExecutor(
            new ShutdownExecutor(new VelocityServerController(
                this.proxy,
                () -> VelocityRestartStrategy.from(this.restartSettings())
            )),
            this::restartSettings
        );
        RestartCountdownPresenter countdownPresenter = new RestartCountdownPresenter(
            scheduler,
            clock,
            this.proxy::getAllPlayers,
            () -> this.restartSettings().countdownSettings()
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
            () -> this.restartSettings().automaticSettings()
        );
        this.automaticRestartManager = automaticRestartManager;
        automaticRestartManager.refresh();

        YaminabeCommands.register(
            this.proxy.getCommandManager(),
            this,
            scheduler,
            this::reload,
            restartService,
            () -> this.restartSettings().commandSettings()
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
        this.restartSettings = null;
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

    private void executeShutdown(VelocityShutdownExecutor executor, ShutdownReservation reservation) {
        executor.execute(reservation).whenComplete((ignored, failure) -> {
            if (failure != null) {
                log().error("Velocity shutdown execution completed exceptionally", failure);
            }
        });
    }

    private VelocityRestartSettings restartSettings() {
        return Objects.requireNonNull(this.restartSettings, "restart settings are not loaded");
    }

    private void loadConfig() throws IOException {
        this.config.reload();
        this.restartSettings = VelocityRestartSettings.from(
            this.config.get().restart(),
            warning -> log().warn(warning)
        );

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
