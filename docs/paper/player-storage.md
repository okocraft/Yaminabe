# Player storage

Each Paper/Folia server owns its own `plugins/Yaminabe/players.db` SQLite database. Velocity does not
read or write it. The initial implementation stores UUID, last known account name, last observed join
and departure, and the token of an unclosed connection. It does not copy vanilla inventories,
ender chests, gamemode or other plugins' data.

## Ordering and failures

`common.player` contains immutable profiles, the typed repository contract and connection tracking.
The Paper SQLite implementation uses one connection on a dedicated single-thread I/O queue. Reads,
join updates, conditional quit updates and resource closure share this queue. There is no profile
cache that a delayed initial read could overwrite. Missing records and failed reads are different
results; failed reads never create or save defaults.

Every join receives a random connection token. A quit only closes the matching session. The Paper
listener distinguishes Player objects by identity, not by UUID or their `equals` implementation,
so a delayed old quit cannot close a reconnect. Online status comes from current connection tracking,
never from stored timestamps. An open token surviving a restart means the latest departure is
unconfirmed; the system does not invent a logout time. This works even if the wall clock moves back.

The initial `/seen` implementation only reads profiles. Its replies to players run on the sender's
entity scheduler; rejected/retired connections receive no reply. No database task waits for a Player
scheduler. Future setting updates must distinguish committed storage from successful online
application, check both connection token and current revision, and compute toggles within the
ordered update operation rather than from a stale snapshot.

## Schema, shutdown and backup

The schema version is SQLite `user_version`. Initialization and transactional migration finish before
commands/listeners are registered. An unknown newer schema or an invalid existing schema prevents
Yaminabe startup; it is never reset to an empty database. Migration failure rolls back the schema and
version together. A failed open closes the connection before returning.

SQLite uses the rollback journal (`DELETE`), `synchronous=FULL` and a five-second busy timeout.
There is no supported multi-process access to a shared database. Durability still depends on the
filesystem and storage honoring synchronization; power loss is not the same as a JVM crash.

On plugin disable, active tracking sessions receive a departure observation, new submissions are
rejected, and accepted operations drain before the connection closes. Shutdown waits up to ten
seconds independently of plugin schedulers. Failures and timeout are logged as unconfirmed writes;
timeout does not pretend that the queue has completed. A tracking departure can therefore be caused
by plugin disable as well as a player disconnect.

Back up the database after a clean server stop with successful storage closure. Do not copy the live
database file alone. `/yaminabe reload` reloads configuration/languages without replacing storage.

## Subsequent features

Add typed columns/repositories with each feature, using the same database queue. Do not expose an
arbitrary key/value store or save a mutable whole-player object from an old snapshot.

| Feature | Persistence policy |
|---------|--------------------|
| `/nick` | Store the nickname definition; render chat/tab output through their owning integration. |
| `/fly` | Only a player's own requested setting may be restored. Settings granted to another player by an administrator are session-only. Check current authorization/gamemode before applying. |
| `/back` | Deferred. Design time-bounded history rather than assuming a permanent single return slot; define retention, successful teleport/death recording and `/back` transitions first. |
| `/socialspy` | Store the toggle only after the target messaging integration is defined. |
| `/afk`, `/vanish` | Session state; join-time auto-vanish, if needed, is a separate policy. |
| `/speed` | Initially operate on Player state without a second database copy; define permission-loss/reset behavior when implementing the command. |
| `/ptime`, `/pweather` | Keep the existing session-only behavior. |

EssentialsX userdata is not automatically imported by this initial implementation. A later importer
must use a fixed backup/version, run without concurrent writers, preserve explicit Yaminabe settings
(including resets to null), and commit per-player/per-feature completion markers with imported
values. Re-running an import must not resurrect a cleared nickname or other cleared setting.
