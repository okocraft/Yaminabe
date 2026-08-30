package net.okocraft.yaminabe.paper.config;

import net.okocraft.yaminabe.common.config.ConfigLoader;
import org.jetbrains.annotations.NotNullByDefault;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@ConfigSerializable
@NotNullByDefault
public class YaminabePaperConfig {

    private static final String FILENAME = "config.yml";

    @Comment("More output to the console.")
    private boolean debug = false;

    @Setting("unregister-commands")
    @Comment("Additional command labels to unregister on server startup. Namespaced labels can also be specified.")
    private List<String> unregisterCommands = List.of();

    public boolean debug() {
        return this.debug;
    }

    public List<String> unregisterCommands() {
        return List.copyOf(this.unregisterCommands);
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
