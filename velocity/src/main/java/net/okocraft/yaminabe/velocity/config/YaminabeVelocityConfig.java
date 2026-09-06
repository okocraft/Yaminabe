package net.okocraft.yaminabe.velocity.config;

import net.kyori.adventure.bossbar.BossBar;
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

        @Comment("Time zone used by restart commands and automatic restart times. Leave empty to use the system default time zone.")
        private String timeZone = "";

        @Comment("Automatic restart schedule.")
        private Scheduled scheduled = new Scheduled();

        @Comment("Actions performed before a restart.")
        private BeforeShutdown beforeRestart = new BeforeShutdown();

        @Comment("Actions performed before a shutdown.")
        private BeforeShutdown beforeShutdown = new BeforeShutdown();

        @Comment("Countdown presentation settings.")
        private Countdown countdown = new Countdown();

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

        public Scheduled scheduled() {
            return this.scheduled;
        }

        public BeforeShutdown beforeRestart() {
            return this.beforeRestart;
        }

        public BeforeShutdown beforeShutdown() {
            return this.beforeShutdown;
        }

        public Countdown countdown() {
            return this.countdown;
        }
    }

    @ConfigSerializable
    public static class Scheduled {

        @Comment("Whether automatic restarts are scheduled.")
        private boolean enabled = true;

        @Comment("Times of day to restart the proxy, in HH:mm (24-hour). The nearest upcoming time is used.")
        private List<String> times = new ArrayList<>(List.of("06:00"));

        @Comment("Countdown duration in seconds used by automatic restarts. Zero disables the countdown.")
        private long countdownSeconds = 60;

        public boolean enabled() {
            return this.enabled;
        }

        public List<String> times() {
            return List.copyOf(this.times);
        }

        public long countdownSeconds() {
            return this.countdownSeconds;
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

    @ConfigSerializable
    public static class Countdown {

        @Comment("Boss bar settings used while a restart or shutdown countdown is active.")
        private BossBarSettings bossBar = new BossBarSettings();

        @Comment("Remaining seconds at which a chat countdown message is broadcast.")
        private List<Integer> broadcastAtSeconds = new ArrayList<>(List.of(60, 30, 10, 5, 4, 3, 2, 1));

        public BossBarSettings bossBar() {
            return this.bossBar;
        }

        public List<Integer> broadcastAtSeconds() {
            return List.copyOf(this.broadcastAtSeconds);
        }
    }

    @ConfigSerializable
    public static class BossBarSettings {

        @Comment("Whether the countdown boss bar is shown.")
        private boolean enabled = true;

        @Comment("Boss bar color.")
        private BossBar.Color color = BossBar.Color.RED;

        @Comment("Boss bar overlay.")
        private BossBar.Overlay overlay = BossBar.Overlay.NOTCHED_10;

        public boolean enabled() {
            return this.enabled;
        }

        public BossBar.Color color() {
            return this.color;
        }

        public BossBar.Overlay overlay() {
            return this.overlay;
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
