package net.okocraft.yaminabe.paper.plugin;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.okocraft.yaminabe.common.PluginStatus;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.language.LanguageProvider;
import net.okocraft.yaminabe.paper.command.YaminabeCommands;
import net.okocraft.yaminabe.paper.config.YaminabePaperConfig;
import net.okocraft.yaminabe.paper.listener.EventListeners;
import net.okocraft.yaminabe.paper.platform.PaperSchedulerProvider;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;
import static net.okocraft.yaminabe.common.YaminabeLogger.logDebug;

public class YaminabePaperPlugin extends JavaPlugin {

    private final PaperSchedulerProvider scheduler;
    private final List<DefaultMessageDefiner> defaultMessages;
    private final YaminabePaperConfig.Holder config;
    private Set<String> startupCommandsToUnregister = Set.of();
    private volatile List<String> registeredCommandLabels = List.of();
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
                    this.startupCommandsToUnregister = Set.copyOf(this.config.get().unregisterCommands());
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
                this.getLifecycleManager().registerEventHandler(
                    LifecycleEvents.COMMANDS.newHandler(event -> {
                        Commands commands = event.registrar();
                        Set<String> additionalCommands = event.cause() == ReloadableRegistrarEvent.Cause.INITIAL
                            ? this.startupCommandsToUnregister
                            : Set.of();
                        YaminabeCommands.register(
                            commands,
                            this.scheduler.async(),
                            this.scheduler.region(),
                            this::reload,
                            additionalCommands,
                            () -> this.registeredCommandLabels
                        );
                        this.registeredCommandLabels = commands.getDispatcher().getRoot().getChildren().stream()
                            .map(node -> node.getName())
                            .sorted()
                            .toList();
                    }).priority(Integer.MAX_VALUE)
                );
                EventListeners.createListeners().forEach(listener -> this.getServer().getPluginManager().registerEvents(listener, this));
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
                HandlerList.unregisterAll(this);
                LanguageProvider.unload();
                return PluginStatus.DISABLED;
            }
        );
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

    private void loadConfig() throws IOException {
        this.config.reload();

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
