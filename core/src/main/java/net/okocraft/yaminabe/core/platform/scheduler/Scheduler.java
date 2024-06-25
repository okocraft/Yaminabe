package net.okocraft.yaminabe.core.platform.scheduler;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

public interface Scheduler {

    void runNow(@NotNull Runnable task);

    void runDelayed(@NotNull Runnable task, @NotNull Duration delay);

    void runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval);

}
