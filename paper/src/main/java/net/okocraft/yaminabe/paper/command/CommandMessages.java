package net.okocraft.yaminabe.paper.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;

final class CommandMessages {

    static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();
    private static final String PREFIX = "yaminabe.command.";

    static final MessageKey.Arg1<String> VERSION_PRINT = DEFINER.define(PREFIX + "version.print", "<green>Yaminabe <aqua><version>").with(version -> Argument.string("version", version));

    static final MessageKey RELOAD_START = DEFINER.define(PREFIX + "reload.start", "<gray>Reloading Yaminabe...");
    static final MessageKey RELOAD_CONFIG_RELOADED = DEFINER.define(PREFIX + "reload.config-reloaded", "<green>Reloaded the config file.");
    static final MessageKey RELOAD_CONFIG_FAILED = DEFINER.define(PREFIX + "reload.config-failed", "<red>Failed to reload the config file. See the console for details.");
    static final MessageKey RELOAD_LANGUAGE_RELOADED = DEFINER.define(PREFIX + "reload.language-reloaded", "<green>Reloaded the language files.");
    static final MessageKey RELOAD_LANGUAGE_FAILED = DEFINER.define(PREFIX + "reload.language-failed", "<red>Failed to reload the language files. See the console for details.");

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

    static final MessageKey.Arg1<String> SIGN_PLAYER_ONLY = DEFINER.define(PREFIX + "sign.player-only", "<red>Only in-game players can use <aqua><command></aqua><red> without a position.").with(command -> Argument.string("command", command));
    static final MessageKey SIGN_NOT_LOOKED_AT = DEFINER.define(PREFIX + "sign.not-looked-at", "<red>You must be looking at a sign.");
    static final MessageKey.Arg3<Integer, Integer, Integer> SIGN_NOT_FOUND = DEFINER.define(PREFIX + "sign.not-found", "<red>There is no sign at <aqua><x> <y> <z></aqua><red>.").with(x -> Argument.numeric("x", x), y -> Argument.numeric("y", y), z -> Argument.numeric("z", z));
    static final MessageKey.Arg3<Integer, Integer, Integer> SIGN_CHUNK_NOT_LOADED = DEFINER.define(PREFIX + "sign.chunk-not-loaded", "<red>The chunk at <aqua><x> <y> <z></aqua><red> is not loaded.").with(x -> Argument.numeric("x", x), y -> Argument.numeric("y", y), z -> Argument.numeric("z", z));
    static final MessageKey SIGN_IS_WAXED = DEFINER.define(PREFIX + "sign.is-waxed", "<red>You are not allowed to edit a waxed sign.");
    static final MessageKey SIGN_EDIT_PREVENTED = DEFINER.define(PREFIX + "sign.edit-prevented", "<red>Editing this sign was prevented.");
    static final MessageKey.Arg1<Integer> SIGN_TOO_LONG = DEFINER.define(PREFIX + "sign.too-long", "<red>Each line must be at most <aqua><limit></aqua><red> characters.").with(limit -> Argument.numeric("limit", limit));
    static final MessageKey.Arg1<Integer> SIGN_LAST_LINE_NOT_EMPTY = DEFINER.define(PREFIX + "sign.last-line-not-empty", "<red>Line <aqua><number></aqua><red> must be empty to insert a line.").with(number -> Argument.numeric("number", number));
    static final MessageKey.Arg2<Integer, Component> SIGN_SET = DEFINER.define(PREFIX + "sign.set", "<gray>Set line <aqua><number></aqua><gray> of the sign to <aqua><line></aqua><gray>.").with(number -> Argument.numeric("number", number), line -> Argument.component("line", line));
    static final MessageKey.Arg2<Integer, Component> SIGN_INSERTED = DEFINER.define(PREFIX + "sign.inserted", "<gray>Inserted <aqua><line></aqua><gray> at line <aqua><number></aqua><gray> of the sign.").with(number -> Argument.numeric("number", number), line -> Argument.component("line", line));
    static final MessageKey.Arg1<Integer> SIGN_REMOVED = DEFINER.define(PREFIX + "sign.removed", "<gray>Removed line <aqua><number></aqua><gray> from the sign.").with(number -> Argument.numeric("number", number));
    static final MessageKey SIGN_CLEARED = DEFINER.define(PREFIX + "sign.cleared", "<gray>Cleared the sign.");
    static final MessageKey.Arg1<Integer> SIGN_CLEARED_LINE = DEFINER.define(PREFIX + "sign.cleared-line", "<gray>Cleared line <aqua><number></aqua><gray> of the sign.").with(number -> Argument.numeric("number", number));
    static final MessageKey SIGN_ALREADY_EMPTY = DEFINER.define(PREFIX + "sign.already-empty", "<red>The sign is already empty.");
    static final MessageKey.Arg1<Integer> SIGN_LINE_ALREADY_EMPTY = DEFINER.define(PREFIX + "sign.line-already-empty", "<red>Line <aqua><number></aqua><red> of the sign is already empty.").with(number -> Argument.numeric("number", number));
    static final MessageKey SIGN_GLOWING_ENABLED = DEFINER.define(PREFIX + "sign.glowing-enabled", "<gray>The text of the sign now glows.");
    static final MessageKey SIGN_GLOWING_DISABLED = DEFINER.define(PREFIX + "sign.glowing-disabled", "<gray>The text of the sign no longer glows.");
    static final MessageKey.Arg1<String> SIGN_COLOR_SET = DEFINER.define(PREFIX + "sign.color-set", "<gray>Set the color of the sign to <aqua><color></aqua><gray>.").with(color -> Argument.string("color", color));
    static final MessageKey.Arg1<String> SIGN_INVALID_COLOR = DEFINER.define(PREFIX + "sign.invalid-color", "<red><aqua><color></aqua><red> is not a dye color.").with(color -> Argument.string("color", color));
    static final MessageKey SIGN_WAXED_ENABLED = DEFINER.define(PREFIX + "sign.waxed-enabled", "<gray>The sign is now waxed.");
    static final MessageKey SIGN_WAXED_DISABLED = DEFINER.define(PREFIX + "sign.waxed-disabled", "<gray>The sign is no longer waxed.");

    static final MessageKey PTIME_TARGET_REQUIRED = DEFINER.define(PREFIX + "ptime.target-required", "<red>Targets must be specified when the command is not executed as a player.");
    static final MessageKey PTIME_NO_TARGETS = DEFINER.define(PREFIX + "ptime.no-targets", "<red>No target players are available.");
    static final MessageKey PTIME_OTHERS_PREVENTED = DEFINER.define(PREFIX + "ptime.others-prevented", "<red>You are not allowed to manage other players' time.");
    static final MessageKey.Arg2<String, String> PTIME_SET = DEFINER.define(PREFIX + "ptime.set", "<gray>Fixed the time to <aqua><time></aqua><gray> for <aqua><players></aqua><gray>.").with(time -> Argument.string("time", time), players -> Argument.string("players", players));
    static final MessageKey.Arg1<String> PTIME_RESET = DEFINER.define(PREFIX + "ptime.reset", "<gray>Reset the time for <aqua><players></aqua><gray>.").with(players -> Argument.string("players", players));
    static final MessageKey.Arg1<String> PTIME_QUERY_NORMAL = DEFINER.define(PREFIX + "ptime.query-normal", "<aqua><player></aqua><gray>: normal").with(player -> Argument.string("player", player));
    static final MessageKey.Arg2<String, String> PTIME_QUERY_FIXED = DEFINER.define(PREFIX + "ptime.query-fixed", "<aqua><player></aqua><gray>: fixed at <aqua><time></aqua>").with(player -> Argument.string("player", player), time -> Argument.string("time", time));
    static final MessageKey.Arg2<String, String> PTIME_QUERY_RELATIVE = DEFINER.define(PREFIX + "ptime.query-relative", "<aqua><player></aqua><gray>: relative offset <aqua><offset></aqua>").with(player -> Argument.string("player", player), offset -> Argument.string("offset", offset));

    static final MessageKey PWEATHER_TARGET_REQUIRED = DEFINER.define(PREFIX + "pweather.target-required", "<red>Targets must be specified when the command is not executed as a player.");
    static final MessageKey PWEATHER_NO_TARGETS = DEFINER.define(PREFIX + "pweather.no-targets", "<red>No target players are available.");
    static final MessageKey PWEATHER_OTHERS_PREVENTED = DEFINER.define(PREFIX + "pweather.others-prevented", "<red>You are not allowed to manage other players' weather.");
    static final MessageKey.Arg2<String, String> PWEATHER_SET = DEFINER.define(PREFIX + "pweather.set", "<gray>Fixed the weather to <aqua><weather></aqua><gray> for <aqua><players></aqua><gray>.").with(weather -> Argument.string("weather", weather), players -> Argument.string("players", players));
    static final MessageKey.Arg1<String> PWEATHER_RESET = DEFINER.define(PREFIX + "pweather.reset", "<gray>Reset the weather for <aqua><players></aqua><gray>.").with(players -> Argument.string("players", players));
    static final MessageKey.Arg1<String> PWEATHER_QUERY_NORMAL = DEFINER.define(PREFIX + "pweather.query-normal", "<aqua><player></aqua><gray>: normal").with(player -> Argument.string("player", player));
    static final MessageKey.Arg2<String, String> PWEATHER_QUERY_FIXED = DEFINER.define(PREFIX + "pweather.query-fixed", "<aqua><player></aqua><gray>: fixed to <aqua><weather></aqua>").with(player -> Argument.string("player", player), weather -> Argument.string("weather", weather));

    static final MessageKey.Arg1<String> SKULL_GIVEN = DEFINER.define(PREFIX + "skull.given", "<gray>Gave the skull of <aqua><owner></aqua><gray>.").with(owner -> Argument.string("owner", owner));
    static final MessageKey SKULL_INVALID_NAME = DEFINER.define(PREFIX + "skull.invalid-name", "<red>The owner must be a player name.");
    static final MessageKey SKULL_INVALID_OWNER = DEFINER.define(PREFIX + "skull.invalid-owner", "<red>The owner must be a player name, a texture hash or a texture value.");
    static final MessageKey SKULL_ONLINE_PREVENTED = DEFINER.define(PREFIX + "skull.online-prevented", "<red>You are not allowed to get the skull of another player.");
    static final MessageKey SKULL_OFFLINE_PREVENTED = DEFINER.define(PREFIX + "skull.offline-prevented", "<red>You are not allowed to get the skull of a player who is not online.");
    static final MessageKey SKULL_TEXTURE_PREVENTED = DEFINER.define(PREFIX + "skull.texture-prevented", "<red>You are not allowed to get a skull from a texture.");
    static final MessageKey.Arg1<String> SKULL_PLAYER_ONLY = DEFINER.define(PREFIX + "skull.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey.Arg1<String> WORKBENCH_PLAYER_ONLY = DEFINER.define(PREFIX + "workbench.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey.Arg1<String> ANVIL_PLAYER_ONLY = DEFINER.define(PREFIX + "anvil.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey.Arg1<String> CARTOGRAPHYTABLE_PLAYER_ONLY = DEFINER.define(PREFIX + "cartographytable.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey.Arg1<String> GRINDSTONE_PLAYER_ONLY = DEFINER.define(PREFIX + "grindstone.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey.Arg1<String> LOOM_PLAYER_ONLY = DEFINER.define(PREFIX + "loom.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey.Arg1<String> SMITHINGTABLE_PLAYER_ONLY = DEFINER.define(PREFIX + "smithingtable.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    static final MessageKey.Arg1<String> STONECUTTER_PLAYER_ONLY = DEFINER.define(PREFIX + "stonecutter.player-only", "<red>Only in-game players can use <aqua><command></aqua><red>.").with(command -> Argument.string("command", command));

    private CommandMessages() {
        throw new UnsupportedOperationException();
    }
}
