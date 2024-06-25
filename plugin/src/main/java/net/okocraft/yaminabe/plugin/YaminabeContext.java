package net.okocraft.yaminabe.plugin;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record YaminabeContext(@NotNull Path dataDirectory)  {
}
