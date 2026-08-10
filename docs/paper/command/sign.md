# `/sign`

Edits a sign in the world. This is a port of the EssentialsX `/editsign` command.

Aliases: `editsign`

## Usage

The sign to edit is the one being looked at, up to 5 blocks away, or the one at a given position.

```
/sign [front|back] <subcommand> ...
/sign at <x> <y> <z> [front|back] <subcommand> ...
```

| Subcommand              | Description                                                                    |
|-------------------------|--------------------------------------------------------------------------------|
| `set <line> <text>`     | Replaces the given line with `<text>`.                                         |
| `insert <line> <text>`  | Inserts `<text>` at the given line, pushing that line and the ones below down. |
| `remove <line>`         | Removes the given line, pulling the ones below it up.                          |
| `clear [line]`          | Clears the given line, or the whole side when no line is given.                |
| `glowing <true\|false>` | Makes the text of the side glow, or stops it from glowing.                     |
| `color <dye>`           | Dyes the text of the side, as a dye does.                                      |
| `waxed <true\|false>`   | Waxes the sign, or removes the wax, as a honeycomb does.                       |

`<line>` is a line that exists, which is 1 to 4. A side of a sign always has four lines, so `insert` needs line 4 to be
empty and reports that it is not rather than dropping what is written on it, and `remove` leaves line 4 empty.

A blank `<text>` is a blank line. It cannot be typed in the chat box, which trims the command, but a command block or a
function can pass one.

`glowing` and `color` are properties of a side, so they apply to the side that was chosen. `waxed` is a property of the
whole sign, so it applies to both sides whichever one was chosen.

### Which sign is edited

Without `at`, the sign is the one being looked at, and this form can only be run by an in-game player. Under
`/execute as <player>`, it is the sign that player is looking at, but every permission below is checked against whoever
issued the command, and the messages go back to them.

With `at`, the sign is the one at that block position, which may be written as `~ ~ ~` or `^ ^ ^` and is then read
relative to wherever the command is run from. This form can also be run by the console or by a command block. The chunk
is never loaded to reach a sign: a position in a chunk that is not loaded is reported instead.

### Which side is edited

Without `front` or `back`, the side is the one facing the player when the sign is being looked at, which is the side
that would be edited by clicking it, and the front when the sign is given by its position.

## Text

`<text>` is written in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format (e.g.
`/sign set 1 <gray>Welcome`). Which tags may be used is decided by the `format` permissions below; tags that are not
allowed are shown as they were typed. See [Format permissions](../../format-permissions.md) for how those permissions
are read and for what they do not constrain.

A line may be at most 15 characters long, which is counted on the text as it is shown rather than on the MiniMessage
source, so the tags themselves do not count towards it. The limit is lifted entirely by
`yaminabe.command.sign.ignore-length-limit`, as a sign can hold a longer line even though it is not shown in full.

Editing the lines of a sign calls the same event as writing on one does, so a plugin that protects a region can refuse
the edit, and one that rewrites what is written on a sign rewrites this as well. The event is only called when the
command is run by or as a player, so lines written by the console or by a command block go through unchecked.

## Waxed signs

A waxed sign cannot be edited in game, and this command does not edit one either unless the sender has
`yaminabe.command.sign.ignore-waxed`. That permission is needed for `set`, `insert`, `remove`, `clear`, `glowing`,
`color` and `waxed false` on a waxed sign. `waxed true` never needs it, as waxing a sign is what anyone holding a
honeycomb can do.

## Tab completion

Pressing tab on `<line>` suggests 1 to 4, and on the `<dye>` of `color` suggests the sixteen dye colors. Pressing tab on
the `<text>` of `set` suggests the current text of that line as MiniMessage, so that it can be edited instead of being
retyped.

The text of a line is not suggested when

- the sign is given by its position, as the sign may be held by another thread that a suggestion cannot wait for,
- accepting the suggestion would not reproduce the line as it is, because it uses a tag the sender may not use or one
  this command does not offer (a sign written by an operator or by another plugin can carry either),
- the line would be longer than 200 characters, or
- the line holds a character the server does not accept in a command, such as a section sign or a line break.

A suggestion is only a shortcut for retyping, so `ignore-length-limit` does not bring back the ones left out.

Under `/execute as <player>`, the suggestions are made from the sign whoever is typing is looking at, not the one the
target player is looking at: the target is only resolved when the command runs, which is after the suggestions are made.

## Permissions

| Permission                                  | Description                                                      |
|---------------------------------------------|------------------------------------------------------------------|
| `yaminabe.command.sign`                     | Required to run `/sign`.                                         |
| `yaminabe.command.sign.at`                  | Allows editing a sign given by its position.                     |
| `yaminabe.command.sign.format.<group>`      | Allows using the MiniMessage tags of the given group in a line.  |
| `yaminabe.command.sign.ignore-length-limit` | Allows writing a line longer than 15 characters.                 |
| `yaminabe.command.sign.ignore-waxed`        | Allows editing a waxed sign and removing the wax from one.       |
| `yaminabe.command.sign.glowing`             | Allows making the text of a sign glow.                           |
| `yaminabe.command.sign.color`               | Allows dyeing the text of a sign.                                |
| `yaminabe.command.sign.waxed`               | Allows waxing a sign and, with `ignore-waxed`, removing the wax. |

### Format groups

`format` works as described in [Format permissions](../../format-permissions.md), with `yaminabe.command.sign.format`
as its base node. The groups offered in a line are:

| Group          | Tags                                                                    |
|----------------|-------------------------------------------------------------------------|
| `color`        | `<red>`, `<color:#ff0000>`, `<#ff0000>`                                 |
| `decoration`   | `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>` |
| `gradient`     | `<gradient>`                                                            |
| `rainbow`      | `<rainbow>`                                                             |
| `transition`   | `<transition>`                                                          |
| `shadow-color` | `<shadow>`                                                              |
| `font`         | `<font>`                                                                |
| `click`        | `<click>`                                                               |
| `translatable` | `<lang>`, `<lang_or>`, `<key>`                                          |
| `reset`        | `<reset>`                                                               |

Tags outside this table are never available, whatever permissions are granted. A line of a sign is rendered as a single
line in the world, so `<newline>`, `<hover>` and `<insert>` are left out, as are the tags that read server state
(`<selector>`, `<score>`, `<nbt>`). Unlike [`/itemlore`](itemlore.md), which offers the same groups otherwise, `click`
is offered here, because a sign acts on a click. Read the next section before granting it.

### `click` on a sign

A sign runs the command of a `<click:run_command:'...'>` on the line that is clicked, so `click` is the one group here
that does more than decide how a line looks. What `yaminabe.command.sign.format.click` hands out is therefore worth
spelling out.

- **The command runs at permission level 2, not as whoever clicks the sign.** That is the level a command block runs at,
  so a player who could not run `/give` themselves runs it by clicking a sign that carries it. Whoever may write a
  `<click>` on a sign can have anyone who clicks it run any command up to that level.
- **A group node says nothing about the arguments of its tags**, as
  [Format permissions](../../format-permissions.md) describes, so there is no way to allow `<click>` while restricting
  which commands may be put in it.
- **Only some actions do anything.** A sign acts on `run_command`, `show_dialog` and `custom`. `open_url`,
  `suggest_command`, `copy_to_clipboard` and `change_page` are written onto the line and then ignored.

Paper calls `PlayerSignCommandPreprocessEvent` before running such a command, so a plugin can still refuse it, and the
command is written to the server log as one the clicking player issued when `settings.log-commands` is on.

## Differences from EssentialsX

- EssentialsX only edits the sign being looked at. Yaminabe also takes a block position, which lets the console and a
  command block edit a sign as well.
- EssentialsX picks the side by where the player is standing. Yaminabe does the same, and also lets the side be named.
- EssentialsX offers `set`, `clear`, `copy` and `paste`. Yaminabe offers `set`, `clear`, `insert` and `remove`, so that
  a line can be edited without retyping the ones after it. `copy` and `paste` are not ported yet.
- EssentialsX has no way to change the glow, the color or the wax of a sign; `/sign glowing`, `/sign color` and
  `/sign waxed` have no EssentialsX counterpart.
- EssentialsX uses legacy color codes (`&c`) gated behind `essentials.editsign.color`, `.format`, `.magic` and `.rgb`,
  and strips the codes the player may not use. Yaminabe uses MiniMessage gated behind
  `yaminabe.command.sign.format.<group>`, and leaves the tags that are not allowed as they were typed.
- EssentialsX counts the 15 characters after stripping the color codes and lifts the limit with
  `essentials.editsign.unlimited`. Yaminabe counts the text as it is shown and lifts the limit with
  `yaminabe.command.sign.ignore-length-limit`.
- EssentialsX edits a waxed sign for a player with `essentials.editsign.waxed.exempt`, and does nothing at all, without
  a word, for anyone else. Yaminabe requires `yaminabe.command.sign.ignore-waxed` for the same, and tells whoever does
  not have it why nothing happened.
- EssentialsX reports success when `clear` is run on a sign that is already empty. Yaminabe reports that it is already
  empty.
