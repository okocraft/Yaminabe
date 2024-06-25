package net.okocraft.yaminabe.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.core.util.YaminabeLogger.log;

public class YaminabePlugin extends JavaPlugin {

    private final @NotNull YaminabeContext context;
    private Status status = Status.NOT_LOADED;

    public YaminabePlugin(@Nullable YaminabeContext context) {
        //noinspection DataFlowIssue
        this.context = context;
        if (context == null) {
            this.status = Status.UNSUPPORTED_PLATFORM;
        }
    }

    @Override
    public void onLoad() {
        this.checkStatusAndRun(
            Status.NOT_LOADED,
            "load",
            () -> {
                return Status.LOADED;
            }
        );
    }

    @Override
    public void onEnable() {
        this.checkStatusAndRun(
            Status.LOADED,
            "enable",
            () -> {
                return Status.ENABLED;
            }
        );
    }

    @Override
    public void onDisable() {
        this.checkStatusAndRun(
            Status.ENABLED,
            "disable",
            () -> {
                return Status.DISABLED;
            }
        );
    }

    private void checkStatusAndRun(@NotNull Status expectedStatus, @NotNull String action, @NotNull Supplier<Status> resultSupplier) {
        if (this.status != expectedStatus) {
            log().error("Cannot {} Yaminabe ({})", action, this.status);
            return;
        }

        var start = Instant.now();
        this.status = resultSupplier.get();
        var finish = Instant.now();

        log().info("Successfully {}! ({}ms)", this.status.name().toLowerCase(Locale.ENGLISH), Duration.between(start, finish).toMillis());
    }

    public enum Status {
        NOT_LOADED,
        LOADED,
        ENABLED,
        DISABLED,
        EXCEPTION_OCCURRED,
        UNSUPPORTED_PLATFORM
    }
}
