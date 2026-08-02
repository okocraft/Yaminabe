package net.okocraft.yaminabe.paper.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.minimessage.translation.Argument;

final class CommandMessages {

    static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();
    private static final String PREFIX = "yaminabe.command.";

    static final MessageKey.Arg1<String> VERSION_PRINT = DEFINER.define(PREFIX + "version.print", "<green>Yaminabe <aqua><version>").with(version -> Argument.string("version", version));

    static final MessageKey DISPOSAL_TITLE = DEFINER.define(PREFIX + "disposal.title", "Disposal");
    static final MessageKey DISPOSAL_OPENING = DEFINER.define(PREFIX + "disposal.opening", "<gray>Opening disposal menu...");
    static final MessageKey.Arg1<String> DISPOSAL_PLAYER_ONLY = DEFINER.define(PREFIX + "disposal.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    private CommandMessages() {
        throw new UnsupportedOperationException();
    }
}
