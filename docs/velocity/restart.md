# Restart management on Velocity

Yaminabe can schedule manual or automatic restarts and shutdowns on Velocity.

## Commands

The main command is `/autorestart`. On Velocity it is also registered as `/are` and `/vare`.

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

`/vrestart [reason <text...>]` schedules an immediate restart.

Durations accept either bare seconds (`90`) or combinations of `d`, `h`, `m`, and `s`, such as `1h30m` or `5m`.

`at` accepts an ISO local date-time such as `2026-09-10T18:00`, or a time of day such as `18:00`. Time-only values use the next occurrence in `restart.time-zone`.

When `countdown` is omitted, `restart.default-countdown-seconds` is used. `countdown full` starts the countdown immediately and keeps it active for the full waiting period.

## Permissions

| Permission | Allows |
| --- | --- |
| `yaminabe.command.restart` | `restart` actions and `/vrestart` |
| `yaminabe.command.stop` | `stop` actions |
| `yaminabe.command.cancel` | cancelling the active reservation |

The `/autorestart` root is only available to sources that have at least one of these permissions. This also prevents the proxy command from claiming the root for players that have no Yaminabe restart permissions.

## Restart modes

Velocity itself does not provide a native process restart operation. Yaminabe supports two modes:

- `SUPERVISOR`: stop Velocity and rely on systemd, Docker, a hosting panel, or another external supervisor to start it again.
- `COMMAND`: register a JVM shutdown hook and launch the configured `restart.command` with `ProcessBuilder` when the proxy exits.

Each entry in `restart.command` is one process argument. For example:

```yaml
restart:
  mode: COMMAND
  command:
    - sh
    - start.sh
```

If `mode` is `COMMAND` but `command` is empty, Yaminabe logs a warning and falls back to `SUPERVISOR` mode for that loaded configuration.

## Manual restart settings

```yaml
restart:
  default-countdown-seconds: 60
  time-zone: Asia/Tokyo

  before-restart:
    commands:
      - alert The proxy is restarting
    kick-players: true

  before-shutdown:
    commands:
      - alert The proxy is shutting down
    kick-players: true
```

Pre-shutdown commands are executed as Velocity console commands, in list order. They are not backend server console commands.

Command failures and player-disconnect failures are best-effort: Yaminabe continues to the final stop or restart action.

## Automatic restart schedule

```yaml
restart:
  time-zone: Asia/Tokyo
  scheduled:
    enabled: true
    times:
      - '06:00'
      - '18:00'
    countdown-seconds: 60
```

Yaminabe schedules the nearest upcoming configured time. Invalid time entries are warned about and skipped. If no valid times remain, automatic restart is disabled while the plugin continues running.

`/yaminabe reload` recomputes an active automatic reservation from the new configuration. A manual reservation is not replaced by reload.

After an explicit cancellation, automatic scheduling resumes from the next configured occurrence strictly after the cancelled reservation's execution time.

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

The countdown display is independent from the execution task. A delayed UI tick does not delay the scheduled restart or shutdown execution.

Players that join while a countdown is active are shown the current BossBar.
