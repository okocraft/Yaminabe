package net.okocraft.yaminabe.velocity.platform.restart;

import net.okocraft.yaminabe.velocity.config.YaminabeVelocityConfig;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static net.okocraft.yaminabe.common.YaminabeLogger.log;

@NotNullByDefault
public interface VelocityRestartStrategy {

    void prepare();

    static VelocityRestartStrategy from(YaminabeVelocityConfig.Restart settings) {
        Objects.requireNonNull(settings);
        return switch (settings.mode()) {
            case SUPERVISOR -> supervisor();
            case COMMAND -> command(settings.command());
        };
    }

    static VelocityRestartStrategy supervisor() {
        return () -> {
        };
    }

    static VelocityRestartStrategy command(List<String> command) {
        return new CommandVelocityRestartStrategy(command);
    }
}

@NotNullByDefault
final class CommandVelocityRestartStrategy implements VelocityRestartStrategy {

    private final List<String> command;
    private final ShutdownHookRegistrar hookRegistrar;
    private final ProcessLauncher processLauncher;

    CommandVelocityRestartStrategy(List<String> command) {
        this(command, Runtime.getRuntime()::addShutdownHook, CommandVelocityRestartStrategy::launch);
    }

    CommandVelocityRestartStrategy(
        List<String> command,
        ShutdownHookRegistrar hookRegistrar,
        ProcessLauncher processLauncher
    ) {
        this.command = List.copyOf(command);
        if (this.command.isEmpty()) {
            throw new IllegalArgumentException("restart command cannot be empty");
        }
        this.hookRegistrar = Objects.requireNonNull(hookRegistrar);
        this.processLauncher = Objects.requireNonNull(processLauncher);
    }

    @Override
    public void prepare() {
        Thread hook = new Thread(() -> {
            try {
                this.processLauncher.launch(this.command);
            } catch (IOException exception) {
                log().error("Failed to start the configured Velocity restart command", exception);
            }
        }, "Yaminabe-Velocity-Restart");
        this.hookRegistrar.register(hook);
    }

    private static void launch(List<String> command) throws IOException {
        new ProcessBuilder(command)
            .inheritIO()
            .start();
    }

    @FunctionalInterface
    interface ShutdownHookRegistrar {
        void register(Thread hook);
    }

    @FunctionalInterface
    interface ProcessLauncher {
        void launch(List<String> command) throws IOException;
    }
}
