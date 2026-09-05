package net.okocraft.yaminabe.common.restart.command;

import net.kyori.adventure.text.ComponentLike;

public interface RestartCommandSource<S> {

    boolean hasPermission(S source, String permission);

    void sendMessage(S source, ComponentLike message);
}
