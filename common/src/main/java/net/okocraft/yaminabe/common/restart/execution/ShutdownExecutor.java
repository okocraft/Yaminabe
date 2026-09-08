package net.okocraft.yaminabe.common.restart.execution;

import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

@NotNullByDefault
public final class ShutdownExecutor {

    public static final Duration DEFAULT_PREPARATION_TIMEOUT = Duration.ofSeconds(10);

    private final ServerController controller;
    private final Duration preparationTimeout;

    public ShutdownExecutor(ServerController controller) {
        this(controller, DEFAULT_PREPARATION_TIMEOUT);
    }

    public ShutdownExecutor(ServerController controller, Duration preparationTimeout) {
        this.controller = Objects.requireNonNull(controller);
        this.preparationTimeout = Objects.requireNonNull(preparationTimeout);
        if (preparationTimeout.isZero() || preparationTimeout.isNegative()) {
            throw new IllegalArgumentException("preparationTimeout must be positive");
        }
    }

    public CompletionStage<Void> executeWithoutKick(ShutdownType type, List<String> commands) {
        return this.executeInternal(type, commands, null);
    }

    public CompletionStage<Void> execute(
        ShutdownType type,
        List<String> commands,
        Component kickReason
    ) {
        return this.executeInternal(type, commands, Objects.requireNonNull(kickReason));
    }

    public CompletionStage<Void> executeFinal(ShutdownType type) {
        Objects.requireNonNull(type);
        try {
            this.executeFinalAction(type);
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException | Error failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private CompletionStage<Void> executeInternal(
        ShutdownType type,
        List<String> commands,
        @Nullable Component kickReason
    ) {
        Objects.requireNonNull(type);
        try {
            return this.executePrepared(type, List.copyOf(commands), kickReason);
        } catch (RuntimeException | Error failure) {
            log().warn(
                "Failed to prepare shutdown execution; proceeding directly to the terminal action",
                failure
            );
            return this.executeFinal(type);
        }
    }

    private CompletionStage<Void> executePrepared(
        ShutdownType type,
        List<String> commandList,
        @Nullable Component kickReason
    ) {
        AtomicBoolean timedOut = new AtomicBoolean();
        AtomicReference<String> phase = new AtomicReference<>();

        CompletionStage<Void> preparation = CompletableFuture.completedFuture(null);
        for (String command : commandList) {
            preparation = preparation.thenCompose(ignored -> {
                if (timedOut.get()) {
                    return CompletableFuture.completedFuture(null);
                }
                phase.set("shutdown command '" + command + "'");
                return this.dispatchBestEffort(command);
            });
        }

        preparation = preparation.handle((ignored, failure) -> null);
        if (kickReason != null) {
            preparation = preparation.thenCompose(ignored -> {
                if (timedOut.get()) {
                    return CompletableFuture.completedFuture(null);
                }
                phase.set("player kick");
                return this.kickBestEffort(kickReason);
            });
        }

        CompletableFuture<Void> preparationDeadline = new CompletableFuture<>();
        AtomicBoolean deadlineCompleted = new AtomicBoolean();
        preparation.whenComplete((ignored, failure) -> {
            if (deadlineCompleted.compareAndSet(false, true)) {
                preparationDeadline.complete(null);
            }
        });
        CompletableFuture.delayedExecutor(
            Math.max(1L, this.preparationTimeout.toMillis()),
            TimeUnit.MILLISECONDS
        ).execute(() -> {
            if (deadlineCompleted.compareAndSet(false, true)) {
                timedOut.set(true);
                log().warn("Timed out while waiting for pre-shutdown {}", phase.get());
                preparationDeadline.complete(null);
            }
        });

        return preparationDeadline.thenCompose(ignored -> this.executeFinal(type));
    }

    private void executeFinalAction(ShutdownType type) {
        switch (type) {
            case RESTART -> this.controller.restart();
            case STOP -> this.controller.stop();
        }
    }

    private CompletionStage<Void> dispatchBestEffort(String command) {
        try {
            return Objects.requireNonNull(this.controller.dispatchConsoleCommand(command))
                .handle((executed, failure) -> {
                    if (failure != null) {
                        log().warn("Failed to execute shutdown command: {}", command, failure);
                    } else if (!Boolean.TRUE.equals(executed)) {
                        log().warn("Shutdown command was not executed successfully: {}", command);
                    }
                    return null;
                });
        } catch (RuntimeException exception) {
            log().warn("Failed to execute shutdown command: {}", command, exception);
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletionStage<Void> kickBestEffort(Component reason) {
        try {
            return Objects.requireNonNull(this.controller.kickAll(reason))
                .handle((ignored, failure) -> {
                    if (failure != null) {
                        log().warn("Failed to kick all players before shutdown", failure);
                    }
                    return null;
                });
        } catch (RuntimeException exception) {
            log().warn("Failed to kick all players before shutdown", exception);
            return CompletableFuture.completedFuture(null);
        }
    }
}
