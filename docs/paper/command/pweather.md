# `/pweather`

Fixes the client-side weather for individual players without changing the world's weather.

## Usage

| Command                       | Description                                                     |
|-------------------------------|-----------------------------------------------------------------|
| `/pweather clear [targets]`   | Fixes the selected players' weather to clear.                   |
| `/pweather rain [targets]`    | Fixes the selected players' weather to rain.                    |
| `/pweather reset [targets]`   | Makes the selected players follow the world weather again.      |
| `/pweather query [targets]`   | Shows the current player-weather override for selected players. |

`[targets]` uses the vanilla player-selector argument, so player names and selectors such as `@s`, `@p`, and `@a` are
supported. If omitted, the command targets the player executing the command. A non-player executor must specify targets.
A selector that resolves to no online players fails without changing anything.

The weather remains fixed until `/pweather reset` is run.

## Permissions

| Permission                              | Description                                     |
|-----------------------------------------|-------------------------------------------------|
| `yaminabe.command.pweather`             | Required to access `/pweather`.                 |
| `yaminabe.command.pweather.set`         | Allows fixing your own player weather.          |
| `yaminabe.command.pweather.set.others`  | Allows fixing other players' weather.           |
| `yaminabe.command.pweather.reset`       | Allows resetting your own player weather.       |
| `yaminabe.command.pweather.reset.others`| Allows resetting other players' weather.        |
| `yaminabe.command.pweather.query`       | Allows querying your own player weather.        |
| `yaminabe.command.pweather.query.others`| Allows querying other players' weather.         |

If a target selector contains at least one player other than the command sender, the corresponding `.others` permission
is required and the entire operation is rejected when it is missing. A non-player sender always requires `.others`.

`/execute` may change the command executor used by an omitted target or `@s`, but it does not change the sender used for
`.others` permission checks. This prevents changing the execution context from granting access to another player's state.

Vanilla selector permissions still apply to selectors parsed by the server.

## Differences from EssentialsX and `/weather`

- Only `clear` and `rain` are supported. Bukkit/Paper's player-weather API has no separate thunderstorm state, so Yaminabe
  does not expose a misleading `thunder` option.
- A duration is intentionally not supported. The override stays fixed until it is reset.
- EssentialsX aliases such as `sun` and `storm` are not supported.
- Player targeting uses vanilla selectors instead of EssentialsX's `*` and `**` handling.
- The command has no EssentialsX aliases such as `playerweather` or `epweather`.
