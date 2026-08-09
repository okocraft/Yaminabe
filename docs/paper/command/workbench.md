# `/workbench`

Opens a crafting table menu without a crafting table block. This is a port of the EssentialsX `/workbench` command.

Aliases: `craft`

## Usage

| Command      | Description                     |
|--------------|---------------------------------|
| `/workbench` | Opens the crafting table menu.  |

This command can only be run by an in-game player.

The menu is not bound to a location, so it stays open wherever the player goes and needs no crafting table block. Items
left in the crafting grid are returned to the player when the menu is closed.

## Permissions

| Permission                   | Description                   |
|------------------------------|-------------------------------|
| `yaminabe.command.workbench` | Required to run `/workbench`. |

## Differences from EssentialsX

- EssentialsX registers `craft`, `wb` and `wbench` as aliases. Yaminabe registers only `craft`.
