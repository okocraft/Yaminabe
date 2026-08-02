# `/hat`

Wears the item in the main hand as a hat. This is a port of the EssentialsX `/hat` command.

Aliases: `head`

## Usage

| Command       | Description                                                       |
|---------------|-------------------------------------------------------------------|
| `/hat`        | Swaps the item in the main hand with the item in the helmet slot. |
| `/hat remove` | Moves the item in the helmet slot back into the inventory.        |

This command can only be run by an in-game player.

`/hat remove` drops the item at the player's feet if there is no free space in the inventory.

## Permissions

| Permission                                   | Description                                                                                              |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `yaminabe.command.hat`                       | Required to run `/hat`.                                                                                  |
| `yaminabe.command.hat.allow-type.<material>` | Allows wearing the given item type. `<material>` is the item's key without the namespace (e.g. `stone`). |
| `yaminabe.command.hat.ignore-binding`        | Allows wearing and removing items enchanted with the Curse of Binding.                                   |

`allow-type` is an allowlist: no item can be worn unless the matching permission is granted. Grant
`yaminabe.command.hat.allow-type.*` and negate individual types (e.g. `-yaminabe.command.hat.allow-type.barrier`) to
forbid specific items.

## Differences from EssentialsX

- EssentialsX rejects any item with durability (`hatArmor`). Yaminabe does not, because an item in the helmet slot only
  changes the player's appearance.
- EssentialsX uses a denylist (`essentials.hat.prevent-type.<material>`). Yaminabe uses an allowlist instead, so that a
  wildcard permission such as `yaminabe.*` does not unintentionally forbid every item.
- EssentialsX sends the same message for an empty hand and for a forbidden item type. Yaminabe distinguishes them.
- EssentialsX also accepts `off` and `0` in place of `remove`. Yaminabe only accepts `remove`.
