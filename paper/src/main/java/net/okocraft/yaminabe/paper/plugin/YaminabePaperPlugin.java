package net.okocraft.yaminabe.paper.plugin;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.okocraft.yaminabe.common.language.LanguageProvider;
import net.okocraft.yaminabe.paper.command.YaminabeCommands;
import net.okocraft.yaminabe.paper.platform.PaperSchedulerProvider;
import net.okocraft.yaminabe.common.PluginStatus;
import net.okocraft.yaminabe.common.platform.scheduler.SchedulerProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

public class YaminabePaperPlugin extends JavaPlugin {

    private final SchedulerProvider scheduler;
    private final List<DefaultMessageDefiner> defaultMessages;
    private PluginStatus status;

    public YaminabePaperPlugin(@NotNull PluginStatus initialStatus, @NotNull List<DefaultMessageDefiner> defaultMessages) {
        this.status = initialStatus;
        this.defaultMessages = defaultMessages;
        this.scheduler = new PaperSchedulerProvider(this);
    }

    @Override
    public void onLoad() {
        this.checkStatusAndRun(
            PluginStatus.NOT_LOADED,
            "load",
            () -> {
                try {
                    LanguageProvider.load(this.getDataPath().resolve("languages"), this.defaultMessages);
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
                this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                    Commands commands = event.registrar();
                    YaminabeCommands.register(commands);
                });
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
                LanguageProvider.unload();
                return PluginStatus.DISABLED;
            }
        );
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
