package net.okocraft.yaminabe.velocity.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.minimessage.translation.Argument;

final class CommandMessages {

    static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();
    private static final String PREFIX = "yaminabe.command.";

    static final MessageKey.Arg1<String> VERSION_PRINT = DEFINER.define(PREFIX + "version.print", "<green>Yaminabe <aqua><version>")
        .with(version -> Argument.string("version", version));

    static final MessageKey RELOAD_START = DEFINER.define(PREFIX + "reload.start", "<gray>Reloading Yaminabe...");
    static final MessageKey RELOAD_CONFIG_RELOADED = DEFINER.define(PREFIX + "reload.config-reloaded", "<green>Reloaded the config file.");
    static final MessageKey RELOAD_CONFIG_FAILED = DEFINER.define(PREFIX + "reload.config-failed", "<red>Failed to reload the config file. See the console for details.");
    static final MessageKey RELOAD_LANGUAGE_RELOADED = DEFINER.define(PREFIX + "reload.language-reloaded", "<green>Reloaded the language files.");
    static final MessageKey RELOAD_LANGUAGE_FAILED = DEFINER.define(PREFIX + "reload.language-failed", "<red>Failed to reload the language files. See the console for details.");

    private CommandMessages() {
        throw new UnsupportedOperationException();
    }
}
