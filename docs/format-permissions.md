# Format permissions

Some commands take text written in [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format, such as
`/itemname <red>Excalibur`. Which tags a player may use there is decided by permissions, and this page describes how
those permissions are read. Every command that takes such text works the same way; only the permission each one places
its nodes under, and the groups it offers at all, differ.

| Command                                  | Base node                          |
|------------------------------------------|------------------------------------|
| [`/itemname`](paper/command/itemname.md) | `yaminabe.command.itemname.format` |

A tag that is not allowed is left exactly as it was typed, and is shown as text. Nothing is rejected and no message is
sent, so a player without any of these permissions can still write `<red>` and get the six characters `<red>`.

## Groups

Each group is enabled by `<base>.<group>`:

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
| `hover`        | `<hover>`                                                               |
| `insertion`    | `<insert>`                                                              |
| `translatable` | `<lang>`, `<lang_or>`, `<key>`                                          |
| `reset`        | `<reset>`                                                               |
| `newline`      | `<newline>`, `<br>`                                                     |

The table lists one way of writing each tag. The aliases MiniMessage accepts (`<c:red>`, `<colour:red>`, `<b>`, `<i>`,
`<em>`, `<u>`, `<st>`, `<obf>`, `<tr>`, ...) and the negated forms (`<!bold>`, ...) are all available with the same
permission, and are checked against the same per-name node.

A command offers only the groups that make sense where its text is used, and the groups it does not offer are never
resolved, whatever permissions are granted. See the page of each command for the groups it offers.

## Individual colors and decorations

A single color or decoration can be allowed or forbidden on its own with `<base>.color.<name>` and
`<base>.decoration.<name>` (e.g. `-yaminabe.command.itemname.format.decoration.obfuscated`). An explicitly set node
takes precedence over the group node; a node that is not set at all falls back to it.

`<name>` is the MiniMessage name of the color or decoration, so `gray` and `dark_gray` cover `<grey>` and `<dark_grey>`
as well. Hexadecimal colors are covered by `<base>.color.hex`.

These nodes only constrain the `color` and `decoration` groups. See below for what that does not cover.

## What these permissions do not constrain

The following are known and accepted. They limit what a per-color denial can be relied on for, so a palette that has to
hold has to be built by denying groups, not individual colors.

- **A denied color can be written as its hexadecimal value.** `<#ff5555>` produces exactly the same color as `<red>`,
  but is checked against `color.hex`, not against `color.red`. Denying an individual color is therefore only meaningful
  where `color.hex` is denied as well. Even then, a color one step away (`#ff5556`) is indistinguishable in game.
- **`<gradient>`, `<rainbow>`, `<transition>` and `<shadow>` resolve their colors inside MiniMessage.** They are
  constrained by their own group node alone, so `<gradient:red:red>` produces red text where `color.red` is denied, and
  `<rainbow>` produces every color without the `color` group at all.
- **A group node says nothing about the arguments of its tags.** `font` allows any font key, `translatable` allows any
  translation key, and so on.
