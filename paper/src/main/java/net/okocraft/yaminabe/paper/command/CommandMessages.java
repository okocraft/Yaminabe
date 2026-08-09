package net.okocraft.yaminabe.paper.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.Component;
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

    static final MessageKey.Arg2<Integer, Component> ITEM_GIVEN = DEFINER.define(PREFIX + "item.given", "<gray>Gave <aqua><amount></aqua> of <aqua><item></aqua><gray>.").with(amount -> Argument.numeric("amount", amount), item -> Argument.component("item", item));
    static final MessageKey.Arg1<String> ITEM_PLAYER_ONLY = DEFINER.define(PREFIX + "item.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey ITEMNAME_NO_ITEM = DEFINER.define(PREFIX + "itemname.no-item", "<red>You must hold an item to rename it.");
    static final MessageKey ITEMNAME_CLEARED = DEFINER.define(PREFIX + "itemname.cleared", "<gray>Cleared the name of the held item.");
    static final MessageKey.Arg1<Component> ITEMNAME_RENAMED = DEFINER.define(PREFIX + "itemname.renamed", "<gray>Renamed the held item to <aqua><name></aqua><gray>.").with(name -> Argument.component("name", name));
    static final MessageKey.Arg1<Component> ITEMNAME_PREVENTED = DEFINER.define(PREFIX + "itemname.prevented", "<red>You are not allowed to rename <aqua><item></aqua><red>.").with(item -> Argument.component("item", item));
    static final MessageKey.Arg1<Integer> ITEMNAME_TOO_LONG = DEFINER.define(PREFIX + "itemname.too-long", "<red>The name must be at most <aqua><limit></aqua><red> characters.").with(limit -> Argument.numeric("limit", limit));
    static final MessageKey.Arg1<String> ITEMNAME_PLAYER_ONLY = DEFINER.define(PREFIX + "itemname.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey ITEMLORE_NO_ITEM = DEFINER.define(PREFIX + "itemlore.no-item", "<red>You must hold an item to edit its lore.");
    static final MessageKey ITEMLORE_NO_LORE = DEFINER.define(PREFIX + "itemlore.no-lore", "<red>The held item does not have any lore.");
    static final MessageKey.Arg1<Integer> ITEMLORE_NO_LINE = DEFINER.define(PREFIX + "itemlore.no-line", "<red>The held item does not have lore on line <aqua><line></aqua><red>.").with(line -> Argument.numeric("line", line));
    static final MessageKey.Arg1<Integer> ITEMLORE_LINE_OUT_OF_RANGE = DEFINER.define(PREFIX + "itemlore.line-out-of-range", "<red>You cannot insert past line <aqua><max></aqua><red> of the lore of the held item.").with(max -> Argument.numeric("max", max));
    static final MessageKey.Arg1<Component> ITEMLORE_ADDED = DEFINER.define(PREFIX + "itemlore.added", "<gray>Added <aqua><line></aqua><gray> to the lore of the held item.").with(line -> Argument.component("line", line));
    static final MessageKey.Arg2<Integer, Component> ITEMLORE_SET = DEFINER.define(PREFIX + "itemlore.set", "<gray>Set line <aqua><number></aqua><gray> of the lore of the held item to <aqua><line></aqua><gray>.").with(number -> Argument.numeric("number", number), line -> Argument.component("line", line));
    static final MessageKey.Arg2<Integer, Component> ITEMLORE_INSERTED = DEFINER.define(PREFIX + "itemlore.inserted", "<gray>Inserted <aqua><line></aqua><gray> at line <aqua><number></aqua><gray> of the lore of the held item.").with(number -> Argument.numeric("number", number), line -> Argument.component("line", line));
    static final MessageKey.Arg1<Integer> ITEMLORE_REMOVED = DEFINER.define(PREFIX + "itemlore.removed", "<gray>Removed line <aqua><number></aqua><gray> from the lore of the held item.").with(number -> Argument.numeric("number", number));
    static final MessageKey ITEMLORE_CLEARED = DEFINER.define(PREFIX + "itemlore.cleared", "<gray>Cleared the lore of the held item.");
    static final MessageKey.Arg1<Component> ITEMLORE_PREVENTED = DEFINER.define(PREFIX + "itemlore.prevented", "<red>You are not allowed to edit the lore of <aqua><item></aqua><red>.").with(item -> Argument.component("item", item));
    static final MessageKey.Arg1<Integer> ITEMLORE_TOO_LONG = DEFINER.define(PREFIX + "itemlore.too-long", "<red>Each line must be at most <aqua><limit></aqua><red> characters.").with(limit -> Argument.numeric("limit", limit));
    static final MessageKey.Arg1<Integer> ITEMLORE_TOO_MANY_LINES = DEFINER.define(PREFIX + "itemlore.too-many-lines", "<red>The lore can have at most <aqua><limit></aqua><red> lines.").with(limit -> Argument.numeric("limit", limit));
    static final MessageKey.Arg1<String> ITEMLORE_PLAYER_ONLY = DEFINER.define(PREFIX + "itemlore.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    private CommandMessages() {
        throw new UnsupportedOperationException();
    }
}
