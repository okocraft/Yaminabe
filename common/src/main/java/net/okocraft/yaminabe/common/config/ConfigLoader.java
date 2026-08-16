package net.okocraft.yaminabe.common.config;

import org.jetbrains.annotations.NotNullByDefault;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

@NotNullByDefault
public final class ConfigLoader<C> {

    private final Path filepath;
    private final Class<C> configClass;
    private final Supplier<C> initialConfig;

    public ConfigLoader(Path filepath, Class<C> configClass, Supplier<C> initialConfig) {
        this.filepath = Objects.requireNonNull(filepath);
        this.configClass = Objects.requireNonNull(configClass);
        this.initialConfig = Objects.requireNonNull(initialConfig);
    }

    public C load() throws IOException {
        var loader = YamlConfigurationLoader.builder()
            .path(this.filepath)
            .nodeStyle(NodeStyle.BLOCK)
            .indent(2)
            .build();
        var node = loader.load();

        if (!node.empty()) {
            if (!node.isMap()) {
                throw new ConfigurateException(node, "The root node of " + this.filepath + " must be a map.");
            }
            return node.require(this.configClass);
        }

        var config = Objects.requireNonNull(this.initialConfig.get(), "initialConfig must not supply null");
        node.set(this.configClass, config);

        var parent = this.filepath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        loader.save(node);
        return config;
    }
}
