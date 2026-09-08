package net.okocraft.yaminabe.paper.plugin;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import net.okocraft.yaminabe.common.PluginStatus;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.language.LanguageProvider;
import net.okocraft.yaminabe.common.restart.AutomaticRestartManager;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownPresenter;
import net.okocraft.yaminabe.common.restart.execution.ShutdownExecutionCoordinator;
import net.okocraft.yaminabe.common.restart.execution.ShutdownExecutor;
import net.okocraft.yaminabe.paper.command.YaminabeCommands;
import net.okocraft.yaminabe.paper.config.PaperRestartSettings;
import net.okocraft.yaminabe.paper.config.YaminabePaperConfig;
import net.okocraft.yaminabe.paper.listener.EventListeners;
import net.okocraft.yaminabe.paper.listener.RestartCountdownListener;
import net.okocraft.yaminabe.paper.platform.PaperSchedulerProvider;
import net.okocraft.yaminabe.paper.platform.restart.PaperRestartCountdownAudience;
import net.okocraft.yaminabe.paper.platform.restart.PaperServerController;
import net.okocraft.yaminabe.paper.platform.restart.PaperShutdownExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;
import static net.okocraft.yaminabe.common.YaminabeLogger.logDebug;

public class YaminabePaperPlugin extends JavaPlugin {

    private final PaperSchedulerProvider scheduler;
    private final List<DefaultMessageDefiner> defaultMessages;
    private final YaminabePaperConfig.Holder config;
    private volatile PaperRestartSettings restartSettings = PaperRestartSettings.from(new YaminabePaperConfig.Restart());
    private volatile @Nullable RestartService restartService;
    private volatile @Nullable AutomaticRestartManager automaticRestartManager;
    private volatile @Nullable RestartCountdownPresenter restartCountdownPresenter;
    private PluginStatus status;

    public YaminabePaperPlugin(@NotNull PluginStatus initialStatus, @NotNull List<DefaultMessageDefiner> defaultMessages) {
        this.status = initialStatus;
        this.defaultMessages = defaultMessages;
        this.scheduler = new PaperSchedulerProvider(this);
        this.config = new YaminabePaperConfig.Holder(this.getDataPath());
    }

    @Override
    public void onLoad() {
        this.checkStatusAndRun(
            PluginStatus.NOT_LOADED,
            "load",
            () -> {
                try {
                    this.loadConfig();
                } catch (Exception e) {
                    log().error("Failed to load the config file", e);
                    return PluginStatus.EXCEPTION_OCCURRED;
                }

                try {
                    this.loadLanguages();
                } catch (Exception e) {
                    log().error("Failed to load language files", e);
                    return PluginStatus.EXCEPTION_OCCURRED;
                }
                return PluginStatus.LOADED;
            }
        );
    }

    @Override
    public void onEnable() {
        this.checkStatusAndRun(
            PluginStatus.LOADED,
            "enable",
            () -> {
                Clock clock = Clock.systemUTC();
                PaperShutdownExecutor shutdownExecutor = new PaperShutdownExecutor(
                    new ShutdownExecutor(new PaperServerController(this, this.scheduler.entity())),
                    () -> this.restartSettings
                );
                RestartCountdownPresenter countdownPresenter = new RestartCountdownPresenter(
                    this.scheduler.async(),
                    clock,
                    new PaperRestartCountdownAudience(this, this.scheduler.entity()),
                    () -> this.restartSettings.countdownSettings()
                );
                this.restartCountdownPresenter = countdownPresenter;

                RestartService restartService = new RestartService(this.scheduler.async(), clock, new RestartService.Listener() {
                    @Override
                    public void onCountdownStarted(ShutdownReservation reservation) {
                        countdownPresenter.start(reservation);
                    }

                    @Override
                    public void onCancelled(ShutdownReservation reservation) {
                        countdownPresenter.stop(reservation);
                        AutomaticRestartManager manager = YaminabePaperPlugin.this.automaticRestartManager;
                        if (manager != null) {
                            manager.onCancelled(reservation);
                        }
                    }

                    @Override
                    public void onExecute(ShutdownReservation reservation) {
                        ShutdownExecutionCoordinator.start(
                            reservation,
                            countdownPresenter::stop,
                            current -> shutdownExecutor.execute(current).whenComplete((ignored, failure) -> {
                                if (failure != null) {
                                    log().error("Paper shutdown execution completed exceptionally", failure);
                                }
                            })
                        );
                    }
                });
                this.restartService = restartService;

                AutomaticRestartManager automaticRestartManager = new AutomaticRestartManager(
                    restartService,
                    clock,
                    () -> this.restartSettings.automaticSettings()
                );
                this.automaticRestartManager = automaticRestartManager;
                automaticRestartManager.refresh();

                boolean folia = ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"));
                this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                    Commands commands = event.registrar();
                    YaminabeCommands.register(
                        commands,
                        this.scheduler.async(),
                        this.scheduler.region(),
                        this.scheduler.entity(),
                        this::reload,
                        restartService,
                        () -> this.restartSettings.commandSettings(),
                        folia
                    );
                });
                EventListeners.createListeners().forEach(listener -> this.getServer().getPluginManager().registerEvents(listener, this));
                this.getServer().getPluginManager().registerEvents(new RestartCountdownListener(countdownPresenter), this);
                return PluginStatus.ENABLED;
            }
        );
    }

    @Override
    public void onDisable() {
        this.checkStatusAndRun(
            PluginStatus.ENABLED,
            "disable",
            () -> {
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
                HandlerList.unregisterAll(this);
                LanguageProvider.unload();
                return PluginStatus.DISABLED;
            }
        );
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

    private void loadConfig() throws IOException {
        this.config.reload();
        this.restartSettings = PaperRestartSettings.from(
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
        LanguageProvider.load(this.getDataPath().resolve("languages"), this.defaultMessages);
    }

    private void checkStatusAndRun(@NotNull PluginStatus expectedStatus, @NotNull String action, @NotNull Supplier<PluginStatus> resultSupplier) {
        if (this.status != expectedStatus) {
            log().error("Cannot {} Yaminabe ({})", action, this.status);
            return;
        }

        var start = Instant.now();
        this.status = resultSupplier.get();
        var finish = Instant.now();

        log().info("Successfully {}! ({}ms)", this.status.name().toLowerCase(Locale.ENGLISH), Duration.between(start, finish).toMillis());
    }

}
