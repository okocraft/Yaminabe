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
import net.okocraft.yaminabe.velocity.command.YaminabeCommands;
import net.okocraft.yaminabe.velocity.config.YaminabeVelocityConfig;
import net.okocraft.yaminabe.velocity.platform.VelocityScheduler;
import org.slf4j.Logger;
import org.slf4j.helpers.SubstituteLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;
import static net.okocraft.yaminabe.common.YaminabeLogger.logDebug;

public final class YaminabeVelocityPlugin {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final YaminabeVelocityConfig.Holder config;

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

        YaminabeCommands.register(
            this.proxy.getCommandManager(),
            this,
            new VelocityScheduler(this.proxy.getScheduler(), this),
            this::reload
        );
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
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

    private void loadConfig() throws IOException {
        this.config.reload();

        boolean debug = this.config.get().debug();
        logDebug(debug);

        if (debug) {
            log().info("Debug mode enabled");
        }
    }

    private void loadLanguages() throws IOException {
        LanguageProvider.load(this.dataDirectory.resolve("languages"), List.of(YaminabeCommands.getDefiner()));
    }
}
