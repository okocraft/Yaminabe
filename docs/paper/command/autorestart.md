# `/autorestart`

Schedules manual or automatic restarts and shutdowns on Paper/Folia.

## Commands

The main command is `/autorestart`, with `/are` as an alias.

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

On Folia, Yaminabe also registers `/restart [reason <text...>]` as an immediate restart command because Folia disables the built-in restart command. On Paper, the built-in `/restart` is left untouched.

Durations accept either bare seconds (`90`) or combinations of `d`, `h`, `m`, and `s`, such as `1h30m` or `5m`.

`at` accepts an ISO local date-time such as `2026-09-10T18:00`, or a time of day such as `18:00`. Time-only values use the next occurrence in `restart.time-zone`.

When `countdown` is omitted, `restart.default-countdown-seconds` is used. `countdown full` starts the countdown immediately and keeps it active for the full waiting period.

## Permissions

| Permission | Allows |
| --- | --- |
| `yaminabe.command.restart` | restart actions and the Folia `/restart` command |
| `yaminabe.command.stop` | stop actions |
| `yaminabe.command.cancel` | cancelling the active reservation |

The `/autorestart` root is only available to sources that have at least one of these permissions.

## Execution settings

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
```

Pre-shutdown commands are executed as server console commands, in list order. Command failures and player-kick failures are best-effort: Yaminabe continues to the final stop or restart action.

On Folia, console/final shutdown operations use the global region scheduler and player kicks use each player's entity scheduler.

## Automatic restart schedule

Automatic restart is disabled by default. Enable it explicitly only after confirming that the server can restart safely.

```yaml
restart:
  time-zone: Asia/Tokyo
  scheduled:
    enabled: false
    times:
      - '06:00'
      - '18:00'
    countdown-seconds: 60
```

Set `scheduled.enabled` to `true` to opt in. Yaminabe schedules the nearest upcoming configured time. Invalid time entries are warned about and skipped. If no valid times remain, automatic restart is disabled while the plugin continues running.

`/yaminabe reload` recomputes an active automatic reservation from the new configuration. A manual reservation is not replaced by reload. If the next automatic execution time is unchanged, the active reservation and countdown remain in place.

Cancelling a manual reservation resumes automatic scheduling from the next occurrence after the current time. Cancelling an automatic reservation skips that cancelled occurrence and resumes strictly after its former execution time.

## Countdown display

```yaml
restart:
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

The countdown display is independent from the execution task. A delayed UI tick does not delay the scheduled restart or shutdown execution. If a display tick crosses multiple configured broadcast thresholds, warnings are sent from the largest remaining time to the smallest.

Players that join while a countdown is active are shown the current BossBar.

See [Restart management on Paper and Folia](../restart.md) for the complete configuration guide.
