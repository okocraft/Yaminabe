# Restart management on Paper and Folia

Yaminabe provides manual restart/shutdown commands, countdown presentation, pre-shutdown actions, and optional automatic restart scheduling on Paper and Folia.

See [`/autorestart`](command/autorestart.md) for command syntax and permissions.

## Manual restart settings

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

`default-countdown-seconds` is used when a manual `restart in` / `restart at` / `stop in` / `stop at` command does not specify its own countdown duration.

`time-zone` is used for time-only `at` arguments and automatic restart times. Leave it empty to use the JVM/system default time zone.

Pre-shutdown commands run in list order. Command failures and player-kick failures are best-effort: Yaminabe still proceeds to the final restart or shutdown action.

On Folia, console commands and final restart/shutdown operations use the global region scheduler. Player operations use each player's entity scheduler.

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

The countdown display is independent from the execution task, so a delayed UI tick does not delay the actual restart or shutdown. If one delayed tick crosses multiple configured broadcast thresholds, messages are emitted from the largest remaining time to the smallest.

Players that join while a countdown is active are shown the current BossBar.

## Automatic restart schedule

Automatic restart is **disabled by default**. This avoids introducing an unexpected daily restart merely by installing or upgrading Yaminabe. Enable it only after verifying that the server's restart mechanism is configured appropriately.

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

Set `scheduled.enabled` to `true` to opt in. Yaminabe schedules the nearest upcoming configured time.

Invalid `HH:mm` entries are logged and skipped. If no valid times remain, automatic restart stays disabled while the plugin continues running. Negative countdown values are clamped to zero.

`/yaminabe reload` recomputes an active automatic reservation from the new configuration. A manual reservation is preserved. If the next automatic execution time is unchanged, the existing automatic reservation and countdown remain in place.

Cancelling a manual reservation resumes automatic scheduling from the next occurrence after the current time. Cancelling an automatic reservation skips the cancelled occurrence and resumes strictly after its former execution time.

## `/restart` on Paper and Folia

Folia disables its native `/restart` command, so Yaminabe registers `/restart [reason <text...>]` there as an immediate Yaminabe restart.

Plain Paper keeps its built-in `/restart` command untouched. To use Yaminabe's pre-restart commands, player kick handling, reason, or countdown behavior on Paper, use `/autorestart restart ...` instead.
