package net.okocraft.yaminabe.common.restart.execution;

import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

@NotNullByDefault
public final class ShutdownExecutor {

    private final ServerController controller;

    public ShutdownExecutor(ServerController controller) {
        this.controller = Objects.requireNonNull(controller);
    }

    public CompletionStage<Void> execute(
        ShutdownType type,
        List<String> commands,
        Component kickReason
    ) {
        Objects.requireNonNull(type);
        List<String> commandList = List.copyOf(commands);
        Objects.requireNonNull(kickReason);

        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (String command : commandList) {
            stage = stage.thenCompose(ignored -> this.dispatchBestEffort(command));
        }

        return stage
            .handle((ignored, failure) -> null)
            .thenRun(() -> this.kickBestEffort(kickReason))
            .thenRun(() -> {
                switch (type) {
                    case RESTART -> this.controller.restart();
                    case STOP -> this.controller.stop();
                }
            });
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

    private void kickBestEffort(Component reason) {
        try {
            this.controller.kickAll(reason);
        } catch (RuntimeException exception) {
            log().warn("Failed to kick all players before shutdown", exception);
        }
    }
}
