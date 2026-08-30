# `/ptime`

Fixes the client-side time for individual players without changing the world's clock.

## Usage

| Command                         | Description                                                  |
|---------------------------------|--------------------------------------------------------------|
| `/ptime set <time> [targets]`   | Fixes the selected players' time to the given time.          |
| `/ptime set day [targets]`      | Fixes the selected players' time to 1000 ticks.              |
| `/ptime set noon [targets]`     | Fixes the selected players' time to 6000 ticks.              |
| `/ptime set night [targets]`    | Fixes the selected players' time to 13000 ticks.             |
| `/ptime set midnight [targets]` | Fixes the selected players' time to 18000 ticks.             |
| `/ptime reset [targets]`        | Makes the selected players follow the world time again.      |
| `/ptime query [targets]`        | Shows the current player-time override for selected players. |

`<time>` uses Minecraft's vanilla `time` argument. For example, `6000`, `6000t`, `30s`, and `1d` are accepted. The
result is normalized to the 24000-tick visual day before it is fixed for the player.

`[targets]` uses the vanilla player-selector argument, so player names and selectors such as `@s`, `@p`, and `@a` are
supported. If omitted, the command targets the player executing the command. A non-player executor must specify targets.
A selector that resolves to no online players fails without changing anything.

The time set by this command is absolute and does not advance. `/ptime reset` restores normal world-time behavior.
The override is session-only and is not persisted by Yaminabe, so disconnecting and joining again also restores normal
world-time behavior. `/ptime query` also reports a relative time override if another plugin has configured one, even
though Yaminabe does not create relative overrides.

## Permissions

| Permission                            | Description                                  |
|---------------------------------------|----------------------------------------------|
| `yaminabe.command.ptime`              | Required to access `/ptime`.                 |
| `yaminabe.command.ptime.set`          | Allows fixing your own player time.          |
| `yaminabe.command.ptime.set.others`   | Allows fixing other players' time.           |
| `yaminabe.command.ptime.reset`        | Allows resetting your own player time.       |
| `yaminabe.command.ptime.reset.others` | Allows resetting other players' time.        |
| `yaminabe.command.ptime.query`        | Allows querying your own player time.        |
| `yaminabe.command.ptime.query.others` | Allows querying other players' time.         |

If a target selector contains at least one player other than the command sender, the corresponding `.others` permission
is required and the entire operation is rejected when it is missing. A non-player sender always requires `.others`.

`/execute` may change the command executor used by an omitted target or `@s`, but it does not change the sender used for
`.others` permission checks. This prevents changing the execution context from granting access to another player's state.

Vanilla selector permissions still apply to selectors parsed by the server.

## Differences from EssentialsX

- Player time is always fixed. EssentialsX's advancing/relative player time is intentionally not supported.
- EssentialsX's `@` prefix for fixed time is unnecessary because fixed time is the only set mode.
- EssentialsX-specific time formats and aliases such as `17:30`, `4pm`, `sunrise`, and `dawn` are not supported.
- Player targeting uses vanilla selectors instead of EssentialsX's `*` and `**` handling.
- The command has no EssentialsX aliases such as `playertime` or `eptime`.
