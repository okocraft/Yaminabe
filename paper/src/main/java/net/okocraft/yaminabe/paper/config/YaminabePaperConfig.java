package net.okocraft.yaminabe.paper.config;

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

        @Comment("Actions performed before a restart.")
        private BeforeShutdown beforeRestart = new BeforeShutdown();

        @Comment("Actions performed before a shutdown.")
        private BeforeShutdown beforeShutdown = new BeforeShutdown();

        public BeforeShutdown beforeRestart() {
            return this.beforeRestart;
        }

        public BeforeShutdown beforeShutdown() {
            return this.beforeShutdown;
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
