package net.okocraft.yaminabe.velocity.config;

import net.okocraft.yaminabe.common.config.ConfigLoader;
import org.jetbrains.annotations.NotNullByDefault;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@ConfigSerializable
@NotNullByDefault
public class YaminabeVelocityConfig {

    private static final String FILENAME = "config.yml";

    @Comment("More output to the console.")
    private boolean debug = false;

    @Comment("Restart and shutdown settings.")
    private Restart restart = new Restart();

    public boolean debug() {
        return this.debug;
    }

    public Restart restart() {
        return this.restart;
    }

    @ConfigSerializable
    public static class Restart {

        @Comment("How Velocity is restarted: SUPERVISOR stops the proxy and relies on an external supervisor; COMMAND launches the configured command when the JVM shuts down.")
        private RestartMode mode = RestartMode.SUPERVISOR;

        @Comment("ProcessBuilder argument list used when mode is COMMAND. Each YAML list entry is one process argument.")
        private List<String> command = new ArrayList<>();

        @Comment("Default countdown duration in seconds used by manual restart and shutdown commands.")
        private long defaultCountdownSeconds = 60;

        @Comment("Time zone used by restart commands. Leave empty to use the system default time zone.")
        private String timeZone = "";

        @Comment("Actions performed before a restart.")
        private BeforeShutdown beforeRestart = new BeforeShutdown();

        @Comment("Actions performed before a shutdown.")
        private BeforeShutdown beforeShutdown = new BeforeShutdown();

        public RestartMode mode() {
            return this.mode;
        }

        public List<String> command() {
            return List.copyOf(this.command);
        }

        public long defaultCountdownSeconds() {
            return this.defaultCountdownSeconds;
        }

        public String timeZone() {
            return this.timeZone;
        }

        public BeforeShutdown beforeRestart() {
            return this.beforeRestart;
        }

        public BeforeShutdown beforeShutdown() {
            return this.beforeShutdown;
        }
    }

    @ConfigSerializable
    public static class BeforeShutdown {

        @Comment("Console commands executed sequentially before the proxy is stopped or restarted.")
        private List<String> commands = new ArrayList<>();

        @Comment("Whether all connected players are disconnected before the proxy is stopped or restarted.")
        private boolean kickPlayers = true;

        public List<String> commands() {
            return List.copyOf(this.commands);
        }

        public boolean kickPlayers() {
            return this.kickPlayers;
        }
    }

    public enum RestartMode {
        SUPERVISOR,
        COMMAND
    }

    public static class Holder {

        private final ConfigLoader<YaminabeVelocityConfig> loader;
        private final AtomicReference<YaminabeVelocityConfig> ref;

        public Holder(Path dataDirectory) {
            this.loader = new ConfigLoader<>(Objects.requireNonNull(dataDirectory).resolve(FILENAME), YaminabeVelocityConfig.class, YaminabeVelocityConfig::new);
            this.ref = new AtomicReference<>(new YaminabeVelocityConfig());
        }

        public YaminabeVelocityConfig get() {
            return this.ref.get();
        }

        public void reload() throws IOException {
            this.ref.set(this.loader.load());
        }
    }
}
