package net.okocraft.yaminabe.paper.config;

import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;

@NotNullByDefault
public record PaperRestartSettings(
    ShutdownSettings beforeRestart,
    ShutdownSettings beforeShutdown
) {

    public PaperRestartSettings {
        Objects.requireNonNull(beforeRestart);
        Objects.requireNonNull(beforeShutdown);
    }

    public static PaperRestartSettings from(YaminabePaperConfig.Restart restart) {
        Objects.requireNonNull(restart);
        return new PaperRestartSettings(
            ShutdownSettings.from(restart.beforeRestart()),
            ShutdownSettings.from(restart.beforeShutdown())
        );
    }

    public ShutdownSettings before(ShutdownType type) {
        Objects.requireNonNull(type);
        return type == ShutdownType.RESTART ? this.beforeRestart : this.beforeShutdown;
    }

    public record ShutdownSettings(List<String> commands, boolean kickPlayers) {

        public ShutdownSettings {
            commands = List.copyOf(commands);
        }

        private static ShutdownSettings from(YaminabePaperConfig.BeforeShutdown settings) {
            return new ShutdownSettings(settings.commands(), settings.kickPlayers());
        }
    }
}
