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

    static final MessageKey HAT_FAIL = DEFINER.define(PREFIX + "hat.fail", "<red>You must have something to wear in your hand.");
    static final MessageKey HAT_PREVENTED = DEFINER.define(PREFIX + "hat.prevented", "<red>You are not allowed to wear this item.");
    static final MessageKey HAT_CURSE = DEFINER.define(PREFIX + "hat.curse", "<red>You cannot remove a hat with the curse of binding.");
    static final MessageKey HAT_EMPTY = DEFINER.define(PREFIX + "hat.empty", "<red>You are not wearing a hat.");
    static final MessageKey HAT_PLACED = DEFINER.define(PREFIX + "hat.placed", "<gray>Enjoy your new hat.");
    static final MessageKey HAT_REMOVED = DEFINER.define(PREFIX + "hat.removed", "<gray>Your hat has been removed.");
    static final MessageKey.Arg1<String> HAT_PLAYER_ONLY = DEFINER.define(PREFIX + "hat.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    private CommandMessages() {
        throw new UnsupportedOperationException();
    }
}
