package net.okocraft.yaminabe.common.platform.scheduler;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

public interface Scheduler {

    void runNow(@NotNull Runnable task);

    @NotNull
    CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay);

    @NotNull
    CancellableTask runAtFixedRate(@NotNull Consumer<CancellableTask> task, @NotNull Duration interval);

}
