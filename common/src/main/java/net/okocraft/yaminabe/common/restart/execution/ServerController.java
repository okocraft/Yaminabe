package net.okocraft.yaminabe.common.restart.execution;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.CompletionStage;

@NotNullByDefault
public interface ServerController {

    CompletionStage<Boolean> dispatchConsoleCommand(String command);

    CompletionStage<Void> kickAll(Component reason);

    void stop();

    void restart();
}
