# `/autorestart`

Schedules manual restarts and shutdowns on Paper/Folia.

Alias: `/are`

```text
/autorestart restart
/autorestart restart now [reason <text...>]
/autorestart restart in <duration> [countdown <duration|full>] [reason <text...>]
/autorestart restart at <date-time> [countdown <duration|full>] [reason <text...>]

/autorestart stop
/autorestart stop now [reason <text...>]
/autorestart stop in <duration> [countdown <duration|full>] [reason <text...>]
/autorestart stop at <date-time> [countdown <duration|full>] [reason <text...>]

/autorestart cancel
```

Durations accept either bare seconds (`90`) or combinations of `d`, `h`, `m`, and `s`, such as `1h30m` or `5m`.

`at` accepts an ISO local date-time such as `2026-09-10T18:00`, or a time of day such as `18:00`. Time-only values use the next occurrence in `restart.time-zone`.

When `countdown` is omitted, `restart.default-countdown-seconds` is used. `countdown full` starts the countdown immediately and keeps it active for the full waiting period.

## `/restart` on Folia

Folia disables the server's native `/restart` command, so Yaminabe registers `/restart [reason <text...>]` there as an immediate Yaminabe restart.

On plain Paper, Yaminabe intentionally does **not** replace Paper's built-in `/restart`. Use `/autorestart restart now` when the Yaminabe pre-restart commands, kick handling, reason, and countdown behavior are required.

## Permissions

| Permission | Allows |
| --- | --- |
| `yaminabe.command.restart` | restart actions and the Folia `/restart` command |
| `yaminabe.command.stop` | stop actions |
| `yaminabe.command.cancel` | cancelling the active reservation |

The `/autorestart` root is only available to sources that have at least one of these permissions.

## Settings

```yaml
restart:
  default-countdown-seconds: 60
  time-zone: Asia/Tokyo

  before-restart:
    commands:
      - save-all
    kick-players: true

  before-shutdown:
    commands:
      - save-all
    kick-players: true

  countdown:
    boss-bar:
      enabled: true
      color: RED
      overlay: NOTCHED_10
    broadcast-at-seconds:
      - 60
      - 30
      - 10
      - 5
      - 4
      - 3
      - 2
      - 1
```

Pre-shutdown commands are executed as server console commands, in list order. Command or kick failures are best-effort: Yaminabe still attempts the final stop or restart action.

The countdown display is independent from the execution task. A delayed UI tick does not delay the scheduled restart or shutdown. Players joining while a countdown is active are shown the current BossBar.
