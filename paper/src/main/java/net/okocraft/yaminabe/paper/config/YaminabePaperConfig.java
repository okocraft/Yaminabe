package net.okocraft.yaminabe.paper.config;

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
public class YaminabePaperConfig {

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

        @Comment("Default countdown duration in seconds used by manual restart and shutdown commands.")
        private long defaultCountdownSeconds = 60;

        @Comment("Time zone used by restart commands. Leave empty to use the system default time zone.")
        private String timeZone = "";

        @Comment("Actions performed before a restart.")
        private BeforeShutdown beforeRestart = new BeforeShutdown();

        @Comment("Actions performed before a shutdown.")
        private BeforeShutdown beforeShutdown = new BeforeShutdown();

        @Comment("Countdown presentation settings.")
        private Countdown countdown = new Countdown();

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

        public Countdown countdown() {
            return this.countdown;
        }
    }

    @ConfigSerializable
    public static class BeforeShutdown {

        @Comment("Console commands executed sequentially before the server is stopped or restarted.")
        private List<String> commands = new ArrayList<>();

        @Comment("Whether all connected players are kicked before the server is stopped or restarted.")
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

    public static class Holder {

        private final ConfigLoader<YaminabePaperConfig> loader;
        private final AtomicReference<YaminabePaperConfig> ref;

        public Holder(Path dataDirectory) {
            this.loader = new ConfigLoader<>(Objects.requireNonNull(dataDirectory).resolve(FILENAME), YaminabePaperConfig.class, YaminabePaperConfig::new);
            this.ref = new AtomicReference<>(new YaminabePaperConfig());
        }

        public YaminabePaperConfig get() {
            return this.ref.get();
        }

        public void reload() throws IOException {
            this.ref.set(this.loader.load());
        }
    }
}
