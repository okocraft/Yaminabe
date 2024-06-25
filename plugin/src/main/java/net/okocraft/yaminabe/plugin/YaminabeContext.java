package net.okocraft.yaminabe.plugin;

import net.kyori.adventure.key.Key;
import net.okocraft.yaminabe.core.module.YaminabeModule;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

public record YaminabeContext(@NotNull Path dataDirectory,
                              @NotNull Map<Key, YaminabeModule.Factory> moduleFactories)  {
}
