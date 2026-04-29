package net.okocraft.yaminabe.common.module;

import net.kyori.adventure.key.Key;
import net.okocraft.yaminabe.common.module.context.DisableContext;
import net.okocraft.yaminabe.common.module.context.EnableContext;
import net.okocraft.yaminabe.common.module.context.InitialContext;
import org.jetbrains.annotations.NotNull;

public interface YaminabeModule {

    void enable(@NotNull EnableContext context);

    void disable(@NotNull DisableContext context);

    interface Factory {
        @NotNull
        YaminabeModule init(@NotNull InitialContext context);
    }

    record Holder(@NotNull Key key, @NotNull YaminabeModule module) {
    }

}
