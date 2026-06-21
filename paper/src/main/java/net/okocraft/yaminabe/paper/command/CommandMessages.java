package net.okocraft.yaminabe.paper.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.minimessage.translation.Argument;

final class CommandMessages {

    static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();
    private static final String PREFIX = "yaminabe.command.";

    static final MessageKey.Arg1<String> VERSION_PRINT = DEFINER.define(PREFIX + "version.print", "<green>Yaminabe <aqua><version>").with(version -> Argument.string("version", version));

    private CommandMessages() {
        throw new UnsupportedOperationException();
    }
}
