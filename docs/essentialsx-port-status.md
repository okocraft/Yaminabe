# EssentialsX Port Status

Status of every command declared in EssentialsX (`Essentials/src/main/resources/plugin.yml`, 153 commands)
against Yaminabe.

| Status                      | Count |
|-----------------------------|-------|
| [Ported](#ported)           | 16    |
| [Planned](#planned)         | 12    |
| [Not planned](#not-planned) | 125   |

Commands from the companion plugins (`EssentialsSpawn`: `/spawn`, `/setspawn`, and `EssentialsChat`,
`EssentialsProtect`, etc.) are out of scope and are not listed here.

Aliases in the tables below are the ones Yaminabe registers, not the full EssentialsX alias list. Per the porting
policy, `e`-prefixed aliases (`edisposal`, `ehat`, ...) are never ported.

## Ported

Already implemented. See [Commands](paper/command/README.md) for details.

| Command             | Yaminabe aliases | Description                      | Notes                                                                                |
|---------------------|------------------|----------------------------------|--------------------------------------------------------------------------------------|
| `/anvil`            | -                | Opens up an anvil.               | The anvil never takes damage, as no anvil block is involved.                         |
| `/cartographytable` | -                | Opens up a cartography table.    | `carttable` is not registered.                                                       |
| `/disposal`         | `trash`          | Opens a portable disposal menu.  |                                                                                      |
| `/editsign`         | -                | Edit a sign in the world.        | Registered as `/sign`, keeping `editsign` as an alias. Text is parsed as MiniMessage. |
| `/grindstone`       | -                | Opens up a grindstone.           |                                                                                      |
| `/hat`              | `head`           | Get some cool new headgear.      | EssentialsX gives `head` to both `/hat` and `/skull`; Yaminabe assigns it to `/hat`. |
| `/item`             | `i`              | Spawn an item.                   |                                                                                      |
| `/itemlore`         | `lore`, `ilore`  | Edit the lore of an item.        | Text is parsed as MiniMessage.                                                       |
| `/itemname`         | `iname`          | Names an item.                   | Text is parsed as MiniMessage.                                                       |
| `/loom`             | -                | Opens up a loom.                 |                                                                                      |
| `/ptime`            | -                | Adjust player's client time.     | Uses vanilla time arguments and player selectors; time is always fixed.              |
| `/pweather`         | -                | Adjust a player's weather.       | Supports `clear` and `rain` with vanilla player selectors.                           |
| `/skull`            | -                | Set the owner of a player skull. | `head` / `playerskull` are not registered (`head` belongs to `/hat`).                |
| `/smithingtable`    | -                | Opens up a smithing table.       | `smithtable` is not registered.                                                      |
| `/stonecutter`      | -                | Opens up a stonecutter.          |                                                                                      |
| `/workbench`        | `craft`          | Opens up a workbench.            | `wb` / `wbench` are not registered.                                                  |

Yaminabe also provides `/yaminabe`, which has no EssentialsX counterpart.

## Planned

Planned to port. Aliases still need to be decided per the policy in `CLAUDE.md`.

| Command       | EssentialsX aliases (excluding `e`-prefixed)                                             | Description                                             |
|---------------|------------------------------------------------------------------------------------------|---------------------------------------------------------|
| `/afk`        | `away`                                                                                   | Marks you as away-from-keyboard.                        |
| `/back`       | `return`                                                                                 | Teleports you to your location prior to tp/spawn/warp.  |
| `/enderchest` | `echest`, `endersee`, `ec`                                                               | Lets you see inside an enderchest.                      |
| `/fly`        | -                                                                                        | Take off, and soar!                                     |
| `/gamemode`   | `gm`, `gma`, `gmc`, `gms`, `gmsp`, `adventure`, `creative`, `survival`, `spectator`, ... | Change player gamemode.                                 |
| `/nick`       | `nickname`                                                                               | Change your nickname or that of another player.         |
| `/seen`       | `alts`                                                                                   | Shows the last logout time of a player.                 |
| `/socialspy`  | -                                                                                        | Toggles if you can see msg/mail commands in chat.       |
| `/speed`      | `flyspeed`, `fspeed`, `walkspeed`, `wspeed`                                              | Change your speed limits.                               |
| `/sudo`       | -                                                                                        | Make another user perform a command.                    |
| `/vanish`     | `v`                                                                                      | Hide yourself from other players.                       |
| `/whois`      | -                                                                                        | Determine basic information about the specified player. |

### Open points

- **`/socialspy`**: in EssentialsX it spies on `/msg` and `/mail`, neither of which is planned for porting. Its target
  (another plugin's private messages? server-wide chat?) has to be defined before implementing it.
- **`/whois`**: the EssentialsX description is "Determine basic information about the specified player"; resolving a
  nickname back to a username is `/realname`, which is not planned. If nickname resolution is the goal, that behaviour
  belongs to `/whois` here, or `/realname` should be added to the plan.
- **`/gamemode`**: `/gamemode` itself exists in vanilla. Only the short aliases (`gm`, `gmc`, ...) and the
  permission-per-mode model are the actual value; the scope of the port should be decided on that basis.
- **`/vanish`**, **`/nick`**, **`/afk`**: these affect chat and the player list, so they need to be aligned with
  whatever plugin currently owns chat formatting and the tab list.

## Not planned

Grouped by the reason for exclusion.

### Covered by vanilla commands (14)

`/clearinventory`, `/enchant`, `/exp`, `/give`, `/kill`, `/spawnmob`, `/time`, `/weather`, `/tp`,
`/tphere`, `/tppos`, `/tpall`, `/tpo`, `/tpohere`

Vanilla `/clear`, `/enchant`, `/xp`, `/give`, `/kill`, `/summon`, `/time`, `/weather` and `/tp` cover these. (`/item`
was still ported because it targets the sender with MiniMessage-aware item metadata.)

### Economy (9)

`/balance`, `/balancetop`, `/eco`, `/pay`, `/paytoggle`, `/payconfirmtoggle`, `/sell`, `/setworth`,
`/worth`

Yaminabe has no economy backend; the economy is owned by a separate plugin.

### Punishment and jail (14)

`/ban`, `/banip`, `/unban`, `/unbanip`, `/tempban`, `/tempbanip`, `/kick`, `/kickall`, `/mute`,
`/jails`, `/jailedplayers`, `/setjail`, `/deljail`, `/togglejail`

Vanilla and the existing moderation tooling cover bans and kicks; the jail system is not used.

### Homes, warps and teleport requests (23)

`/home`, `/sethome`, `/delhome`, `/renamehome`, `/warp`, `/setwarp`, `/delwarp`, `/warpinfo`,
`/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpaall`, `/tpacancel`, `/tpauto`, `/tptoggle`,
`/tpr`, `/settpr`, `/tpoffline`, `/top`, `/bottom`, `/jump`, `/world`

A whole teleport/waypoint subsystem with its own persistence. `/back` is the single exception on the plan.

### Chat, messaging and server information for players (18)

`/msg`, `/r`, `/rtoggle`, `/msgtoggle`, `/mail`, `/me`, `/helpop`, `/ignore`, `/broadcast`,
`/broadcastworld`, `/customtext`, `/motd`, `/rules`, `/info`, `/help`, `/list`, `/near`, `/realname`

Chat and private messaging are owned by another plugin; `/help` and `/list` exist in vanilla. See the `/socialspy` and
`/whois` open points above.

### Fun and troll commands (13)

`/antioch`, `/beezooka`, `/kittycannon`, `/nuke`, `/fireball`, `/lightning`, `/thunder`, `/burn`,
`/ext`, `/ice`, `/suicide`, `/tree`, `/bigtree`

Not utilities; several of them are destructive.

### Kits (5)

`/kit`, `/createkit`, `/delkit`, `/kitreset`, `/showkit`

Requires kit definitions, cooldown persistence and a config format of its own.

### Player and item manipulation utilities (18)

`/god`, `/heal`, `/feed`, `/rest`, `/repair`, `/more`, `/condense`, `/unlimited`, `/powertool`,
`/powertoollist`, `/powertooltoggle`, `/invsee`, `/book`, `/firework`, `/potion`, `/break`,
`/remove`, `/spawner`

Either cheat-grade shortcuts or niche editors. Revisit individually if a concrete need appears (`/invsee` and `/repair`
are the most likely candidates).

### Server administration and diagnostics (11)

`/gc`, `/ping`, `/essentials`, `/backup`, `/playtime`, `/getpos`, `/depth`, `/compass`, `/itemdb`,
`/recipe`, `/clearinventoryconfirmtoggle`

Server metrics, backups and reload handling belong to the server operator's tooling, not to this plugin.
`/yaminabe` already covers Yaminabe's own management needs.
