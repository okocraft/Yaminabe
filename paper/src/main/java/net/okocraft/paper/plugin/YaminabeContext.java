package net.okocraft.paper.plugin;

import net.kyori.adventure.key.Key;
import net.okocraft.yaminabe.common.module.YaminabeModule;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

public record YaminabeContext(@NotNull Path dataDirectory,
                              @NotNull Map<Key, YaminabeModule.Factory> moduleFactories)  {
}
