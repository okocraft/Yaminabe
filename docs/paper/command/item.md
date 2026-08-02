# `/item`

Gives an item to yourself. This is a port of the EssentialsX `/item` command.

Aliases: `i`

## Usage

| Command                  | Description                                          |
|--------------------------|------------------------------------------------------|
| `/item <item>`           | Gives one of the specified item.                     |
| `/item <item> <amount>`  | Gives the specified amount of the specified item.    |

This command can only be run by an in-game player.

The item is specified with the vanilla syntax, so data components can be given as in `/give`
(e.g. `/item diamond_sword[enchantments={sharpness:5}]`).

`<amount>` accepts `1` to `6400`. Amounts larger than the maximum stack size are split into multiple stacks.
Items that do not fit in the inventory are dropped at the player's feet.

## Permissions

| Permission              | Description              |
|-------------------------|--------------------------|
| `yaminabe.command.item` | Required to run `/item`. |

## Differences from EssentialsX

- EssentialsX resolves item names through its own item database (`diamondsword`, numeric IDs, custom aliases) and has
  its own `itemmeta` syntax. Yaminabe uses the vanilla item argument instead, which provides tab completion and data
  component support.
- EssentialsX can restrict which items may be obtained per type (`essentials.itemspawn.item-<name>`). Yaminabe does not;
  `yaminabe.command.item` grants access to every item.
- EssentialsX can change the default amount and hand out oversized stacks through its config
  (`default-stack-size`, `oversized-stacksize`). Yaminabe always defaults to one item and never exceeds the maximum
  stack size.
