package net.okocraft.yaminabe.velocity.config;

import net.okocraft.yaminabe.common.config.ConfigLoader;
import org.jetbrains.annotations.NotNullByDefault;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@ConfigSerializable
@NotNullByDefault
public class YaminabeVelocityConfig {

    private static final String FILENAME = "config.yml";

    @Comment("More output to the console.")
    private boolean debug = false;

    public boolean debug() {
        return this.debug;
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
