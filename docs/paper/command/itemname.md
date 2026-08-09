# `/itemname`

Renames the item in the main hand. This is a port of the EssentialsX `/itemname` command.

Aliases: `iname`

## Usage

| Command            | Description                                 |
|--------------------|---------------------------------------------|
| `/itemname`        | Clears the name of the held item.           |
| `/itemname <name>` | Sets the name of the held item to `<name>`. |

A blank `<name>` clears the name as well. It cannot be typed in the chat box, which trims the command, but a command
block or a function can pass one.

This command can only be run by an in-game player.

Under `/execute as <player>`, the renamed item is the one held by that player, but every permission below is checked
against whoever issued the command, and the messages go back to them.

The name is written in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format (e.g.
`/itemname <red>Excalibur`). Which tags may be used is decided by the `format` permissions below; tags that are not
allowed are shown as they were typed. See [Format permissions](../../format-permissions.md) for how those permissions
are read and for what they do not constrain.

The name is not italicised unless the italic decoration is set explicitly.

The name may be at most 200 characters long, counted on the MiniMessage source as it is typed rather than on the
rendered text, so that the whole command fits in the chat input box. A longer name is rejected with a message, unless
the sender has `yaminabe.command.itemname.ignore-length-limit`.

Pressing tab on the `<name>` argument suggests the current name of the held item as MiniMessage, so that it can be
edited instead of being retyped. The `<!italic>` this command adds is left out of the suggestion. Nothing is suggested
when

- the held item has no name, or its type is not allowed by `allow-type`,
- accepting the suggestion would not reproduce the name as it is, because it uses a tag the sender may not use or one
  this command does not offer (a name set by an operator or by another plugin can carry either),
- the name would be longer than 200 characters, or
- the name holds a character the server does not accept in a command, such as a section sign or a line break.

A suggestion is only a shortcut for retyping, so `ignore-length-limit` does not bring back the ones left out.

Under `/execute as <player>`, the suggestion is the name of the item held by whoever is typing, not the one held by the
target player: the target is only resolved when the command runs, which is after the suggestion is made.

## Permissions

| Permission                                        | Description                                                                                               |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `yaminabe.command.itemname`                       | Required to run `/itemname`.                                                                              |
| `yaminabe.command.itemname.allow-type.<material>` | Allows renaming the given item type. `<material>` is the item's key without the namespace (e.g. `stone`). |
| `yaminabe.command.itemname.format.<group>`        | Allows using the MiniMessage tags of the given group in the name.                                         |
| `yaminabe.command.itemname.ignore-length-limit`   | Allows setting a name longer than 200 characters.                                                         |

`allow-type` is an allowlist: no item can be renamed unless the matching permission is granted. Grant
`yaminabe.command.itemname.allow-type.*` and negate individual types (e.g.
`-yaminabe.command.itemname.allow-type.written_book`) to forbid specific items.

### Format groups

`format` works as described in [Format permissions](../../format-permissions.md), with
`yaminabe.command.itemname.format` as its base node. The groups offered in an item name are:

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

Tags outside this table are never available, whatever permissions are granted. An item name is rendered as a single,
non-interactive line, so `<newline>`, `<click>`, `<hover>` and `<insert>` are left out, as are the tags that read server
state (`<selector>`, `<score>`, `<nbt>`).

## Differences from EssentialsX

- EssentialsX uses legacy color codes (`&c`) gated behind `essentials.itemname.color`, `.format`, `.magic` and `.rgb`,
  with per-color overrides such as `essentials.itemname.red`. Yaminabe uses MiniMessage gated behind
  `yaminabe.command.itemname.format.<group>`, with the same kind of per-color and per-decoration overrides.
- EssentialsX uses a denylist (`essentials.itemname.prevent-type.<name>`). Yaminabe uses an allowlist instead, so that a
  wildcard permission such as `yaminabe.*` does not unintentionally forbid every item.
- EssentialsX leaves the name italicised, as vanilla renders custom names in italic by default. Yaminabe disables the
  italic decoration unless it is set explicitly.
