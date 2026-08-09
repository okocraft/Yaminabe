# `/itemlore`

Edits the lore of the item in the main hand. This is a port of the EssentialsX `/itemlore` command.

Aliases: `lore`, `ilore`

## Usage

| Command                          | Description                                                                       |
|----------------------------------|-----------------------------------------------------------------------------------|
| `/itemlore add <text>`           | Appends `<text>` to the lore of the held item.                                    |
| `/itemlore set <line> <text>`    | Replaces the given line of the lore with `<text>`.                                |
| `/itemlore insert <line> <text>` | Inserts `<text>` at the given line, pushing that line and the ones below it down. |
| `/itemlore remove <line>`        | Removes the given line from the lore.                                             |
| `/itemlore clear`                | Removes the whole lore.                                                           |

`<line>` is counted from 1. `set` and `remove` take a line that exists; `insert` also takes the line after the last one,
which appends as `add` does. `remove`ing the last remaining line removes the lore itself, as `clear` does.

A blank `<text>` is a blank line, which can be used as a spacer. It cannot be typed in the chat box, which trims the
command, but a command block or a function can pass one.

This command can only be run by an in-game player.

Under `/execute as <player>`, the edited item is the one held by that player, but every permission below is checked
against whoever issued the command, and the messages go back to them.

`<text>` is written in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format (e.g.
`/itemlore add <gray>A sword of legend`). Which tags may be used is decided by the `format` permissions below; tags that
are not allowed are shown as they were typed. See [Format permissions](../../format-permissions.md) for how those
permissions are read and for what they do not constrain.

A line is not italicised unless the italic decoration is set explicitly.

The lore may have at most 10 lines, and each line may be at most 200 characters long. The length is counted on the
MiniMessage source as it is typed rather than on the rendered text, so that the whole command fits in the chat input
box. `add` and `insert` are rejected with a message when either limit is reached, unless the sender has
`yaminabe.command.itemlore.ignore-line-limit` or `yaminabe.command.itemlore.ignore-length-limit` respectively. `set`
only checks the length, as it does not add a line.

`ignore-line-limit` only raises the limit to 256 lines, which is as many as an item can hold. There is no such ceiling
on the length of a line, and a line that uses a tag such as `<rainbow>` is rendered as far more text than it is typed
as, so `ignore-length-limit` is worth granting sparingly.

## Tab completion

Pressing tab on `<line>` suggests the line numbers the subcommand accepts, and pressing tab on the `<text>` of `set`
suggests the current text of that line as MiniMessage, so that it can be edited instead of being retyped. The
`<!italic>` this command adds is left out of the suggestion.

Nothing at all is suggested, line numbers included, when the main hand is empty or the held item's type is not allowed
by `allow-type`. The text of a line is additionally not suggested when

- accepting the suggestion would not reproduce the line as it is, because it uses a tag the sender may not use or one
  this command does not offer (lore set by an operator or by another plugin can carry either),
- the line would be longer than 200 characters, or
- the line holds a character the server does not accept in a command, such as a section sign or a line break.

A suggestion is only a shortcut for retyping, so `ignore-length-limit` does not bring back the ones left out.

Under `/execute as <player>`, the suggestions are made from the item held by whoever is typing, not the one held by the
target player: the target is only resolved when the command runs, which is after the suggestions are made.

## Permissions

| Permission                                        | Description                                                                                                          |
|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `yaminabe.command.itemlore`                       | Required to run `/itemlore`.                                                                                         |
| `yaminabe.command.itemlore.allow-type.<material>` | Allows editing the lore of the given item type. `<material>` is the item's key without the namespace (e.g. `stone`). |
| `yaminabe.command.itemlore.format.<group>`        | Allows using the MiniMessage tags of the given group in a line.                                                      |
| `yaminabe.command.itemlore.ignore-length-limit`   | Allows setting a line longer than 200 characters.                                                                    |
| `yaminabe.command.itemlore.ignore-line-limit`     | Allows a lore of more than 10 lines.                                                                                 |

`allow-type` is an allowlist: the lore of no item can be edited unless the matching permission is granted. Grant
`yaminabe.command.itemlore.allow-type.*` and negate individual types (e.g.
`-yaminabe.command.itemlore.allow-type.written_book`) to forbid specific items. This assumes a permission plugin that
resolves such a wildcard and lets the more specific node win, as LuckPerms does; the nodes are not registered with
Bukkit, so plain Bukkit does not expand `*` on its own.

### Format groups

`format` works as described in [Format permissions](../../format-permissions.md), with
`yaminabe.command.itemlore.format` as its base node. The groups offered in a lore line are:

| Group          | Tags                                                                    |
|----------------|-------------------------------------------------------------------------|
| `color`        | `<red>`, `<color:#ff0000>`, `<#ff0000>`                                 |
| `decoration`   | `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>` |
| `gradient`     | `<gradient>`                                                            |
| `rainbow`      | `<rainbow>`                                                             |
| `transition`   | `<transition>`                                                          |
| `shadow-color` | `<shadow>`                                                              |
| `font`         | `<font>`                                                                |
| `translatable` | `<lang>`, `<lang_or>`, `<key>`                                          |
| `reset`        | `<reset>`                                                               |

Tags outside this table are never available, whatever permissions are granted. A lore line is rendered as a single,
non-interactive line, so `<newline>`, `<click>`, `<hover>` and `<insert>` are left out, as are the tags that read server
state (`<selector>`, `<score>`, `<nbt>`). This is the same set of groups as [`/itemname`](itemname.md) offers.

## Differences from EssentialsX

- EssentialsX offers `add`, `set` and `clear` only. Yaminabe also offers `insert` and `remove`, so that a line can be
  edited without retyping the ones after it.
- EssentialsX uses legacy color codes (`&c`) gated behind `essentials.itemlore.color`, `.format`, `.magic` and `.rgb`,
  with per-color overrides such as `essentials.itemlore.red`, and strips the codes the player may not use. Yaminabe uses
  MiniMessage gated behind `yaminabe.command.itemlore.format.<group>`, with the same kind of per-color and
  per-decoration overrides, and leaves the tags that are not allowed as they were typed.
- EssentialsX has no per-item restriction on this command. Yaminabe uses the same kind of allowlist as
  [`/itemname`](itemname.md) does, so that the two commands can be restricted together.
- EssentialsX takes the maximum number of lines from the `max-itemlore-lines` setting (10 by default) and bypasses it
  with `essentials.itemlore.bypass`. Yaminabe fixes the limit at 10 and raises it to 256 with
  `yaminabe.command.itemlore.ignore-line-limit`. EssentialsX has no limit on the length of a line.
- EssentialsX trims the text it is given and requires `add` to be given some, so a blank line cannot be added. Yaminabe
  keeps the text as it was typed, so that a line can be indented or left blank as a spacer.
- EssentialsX leaves a line italicised, as vanilla renders lore in italic by default. Yaminabe disables the italic
  decoration unless it is set explicitly.
- EssentialsX reports success when `clear` is run on an item that has no lore. Yaminabe reports that there is no lore.
