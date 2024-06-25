package net.okocraft.yaminabe.plugin;

import net.okocraft.yaminabe.core.module.YaminabeModule;
import net.okocraft.yaminabe.core.module.context.DisableContext;
import net.okocraft.yaminabe.core.module.context.EnableContext;
import net.okocraft.yaminabe.core.module.context.InitialContext;
import net.okocraft.yaminabe.core.platform.scheduler.SchedulerProvider;
import net.okocraft.yaminabe.platform.PaperListenerRegistrar;
import net.okocraft.yaminabe.platform.PaperSchedulerProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static net.okocraft.yaminabe.core.util.YaminabeLogger.debug;
import static net.okocraft.yaminabe.core.util.YaminabeLogger.log;

public class YaminabePlugin extends JavaPlugin {

    private final @NotNull YaminabeContext context;
    private final SchedulerProvider schedulers;
    private final List<YaminabeModule.Holder> modules = new ArrayList<>();
    private Status status = Status.NOT_LOADED;

    public YaminabePlugin(@Nullable YaminabeContext context) {
        if (context == null) {
            this.status = Status.UNSUPPORTED_PLATFORM;
        }

        //noinspection DataFlowIssue
        this.context = context;
        this.schedulers = new PaperSchedulerProvider(this);
    }

    @Override
    public void onLoad() {
        this.checkStatusAndRun(
            Status.NOT_LOADED,
            "load",
            () -> {
                /* Module Initialization */
                // TODO: setting to disable modules, dependency, and others...
                var moduleInitContext = new InitialContext(this.context.dataDirectory());
                for (var entry : this.context.moduleFactories().entrySet()) {
                    try {
                        debug().info("Initializing {}", entry.getKey());
                        this.modules.add(new YaminabeModule.Holder(entry.getKey(), entry.getValue().init(moduleInitContext)));
                    } catch (Throwable e) {
                        log().error("Failed to initialize {}", entry.getKey(), e);
                    }
                }
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
                var enableContext = new EnableContext(new PaperListenerRegistrar(this), this.schedulers);
                for (var holder : this.modules) {
                    debug().info("Enabling {}", holder.key());
                    holder.module().enable(enableContext);
                }
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
                var disableContext = new DisableContext(new PaperListenerRegistrar(this));
                for (var holder : this.modules.reversed()) {
                    debug().info("Disabling {}", holder.key());
                    holder.module().disable(disableContext);
                }
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
