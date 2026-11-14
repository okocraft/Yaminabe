# Player storage

Each Paper/Folia server owns `plugins/Yaminabe/players.db`. The initial schema stores exactly
three player fields: `uuid` (primary key), `name` (last observed account name), and `updated_at`
(the time of that identity update, stored as UTC epoch milliseconds). A join captures these values
on the player event thread and submits an upsert. Names are not unique identities.

The common module exposes an immutable `PlayerProfile` and a typed asynchronous repository with
UUID lookup, upsert and closure. The Paper implementation runs reads and writes in submission order
on one dedicated SQLite connection/executor. Upserts complete after commit. An absent record and a
storage failure are distinct results. No database task accesses Player state or waits for a plugin
scheduler. There is no profile cache, connection/session tracking, join/quit history or new command.

The initial schema is versioned through SQLite `user_version`. Initialization is transactional;
an invalid existing schema or unknown newer version prevents startup without resetting data.
SQLite uses `journal_mode=DELETE`, `synchronous=FULL`, and a five-second busy timeout. Durability
also depends on the filesystem honoring synchronization. The database is not shared with Velocity
or other server processes. Pre-release databases from earlier revisions of this PR are incompatible
and are rejected rather than automatically replaced.

On disable, new submissions are rejected and accepted operations drain before the connection closes.
Shutdown waits up to ten seconds independently of plugin schedulers and logs failures/timeouts as
unconfirmed writes. `/yaminabe reload` leaves storage open. Back up the database after a clean server
stop with successful storage closure; do not copy the live file alone.
