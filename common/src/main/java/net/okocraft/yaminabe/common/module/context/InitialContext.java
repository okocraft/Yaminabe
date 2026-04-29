package net.okocraft.yaminabe.common.module.context;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record InitialContext(@NotNull Path dataDirectory) {
}
