package net.okocraft.yaminabe.paper.plugin;

import net.okocraft.yaminabe.paper.platform.PaperSchedulerProvider;
import net.okocraft.yaminabe.common.PluginStatus;
import net.okocraft.yaminabe.common.platform.scheduler.SchedulerProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

public class YaminabePaperPlugin extends JavaPlugin {

    private final SchedulerProvider scheduler;
    private PluginStatus status;

    public YaminabePaperPlugin(@NotNull PluginStatus initialStatus) {
        this.status = initialStatus;
        this.scheduler = new PaperSchedulerProvider(this);
    }

    @Override
    public void onLoad() {
        this.checkStatusAndRun(
            PluginStatus.NOT_LOADED,
            "load",
            () -> {
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
