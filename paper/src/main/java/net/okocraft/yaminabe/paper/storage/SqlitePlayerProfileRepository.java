package net.okocraft.yaminabe.paper.storage;

import net.okocraft.yaminabe.common.player.PlayerProfile;
import net.okocraft.yaminabe.common.player.PlayerProfileRepository;
import org.sqlite.JDBC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One connection and one queue for all access, including reads, migrations and resource closure. */
public final class SqlitePlayerProfileRepository implements PlayerProfileRepository {

    private static final int SCHEMA_VERSION = 1;
    private final Connection connection;
    private final ExecutorService executor;
    private CompletableFuture<Void> closing;

    public static SqlitePlayerProfileRepository open(Path file) throws IOException, SQLException {
        Path absolute = file.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        // Construct the driver explicitly: do not depend on shaded service-file discovery.
        Connection connection = new JDBC().connect("jdbc:sqlite:" + absolute, new Properties());
        try {
            initialize(connection);
            return new SqlitePlayerProfileRepository(connection);
        } catch (SQLException | RuntimeException | Error failure) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    private SqlitePlayerProfileRepository(Connection connection) {
        this.connection = connection;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "yaminabe-player-storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void initialize(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            // Rollback journal avoids a multi-file live-backup contract. Never copy a live database anyway.
            statement.execute("PRAGMA journal_mode = DELETE");
            statement.execute("PRAGMA synchronous = FULL");
            statement.execute("PRAGMA busy_timeout = 5000");
            int version;
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                result.next();
                version = result.getInt(1);
            }
            if (version > SCHEMA_VERSION) {
                throw new SQLException("Unsupported player database schema: " + version);
            }
            if (version == SCHEMA_VERSION) {
                // Validate before accepting work; an invalid database must not become an empty one.
                statement.executeQuery("SELECT uuid, last_known_name, name_key, last_login_at, last_logout_at, session_id FROM player_profiles LIMIT 0").close();
                return;
            }
            connection.setAutoCommit(false);
            try {
                statement.execute("""
                    CREATE TABLE player_profiles (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        last_known_name TEXT NOT NULL,
                        name_key TEXT NOT NULL,
                        last_login_at INTEGER NOT NULL,
                        last_logout_at INTEGER,
                        session_id TEXT
                    )
                    """);
                statement.execute("CREATE INDEX player_profiles_name ON player_profiles(name_key)");
                statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> find(UUID uuid) {
        return this.submit(() -> this.read(uuid));
    }

    @Override
    public CompletableFuture<List<PlayerProfile>> findByName(String name) {
        return this.submit(() -> {
            try (var statement = this.connection.prepareStatement("SELECT * FROM player_profiles WHERE name_key = ? ORDER BY uuid")) {
                statement.setString(1, name.toLowerCase(Locale.ROOT));
                try (var result = statement.executeQuery()) {
                    var profiles = new ArrayList<PlayerProfile>();
                    while (result.next()) {
                        profiles.add(profile(result));
                    }
                    return List.copyOf(profiles);
                }
            }
        });
    }

    @Override
    public CompletableFuture<PlayerProfile> recordJoin(UUID uuid, String name, UUID sessionId, Instant at) {
        return this.submit(() -> {
            this.connection.setAutoCommit(false);
            try {
                try (var statement = this.connection.prepareStatement("""
                    INSERT INTO player_profiles(uuid, last_known_name, name_key, last_login_at, session_id)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET last_known_name = excluded.last_known_name,
                        name_key = excluded.name_key, last_login_at = excluded.last_login_at,
                        session_id = excluded.session_id
                    """)) {
                    statement.setString(1, uuid.toString());
                    statement.setString(2, name);
                    statement.setString(3, name.toLowerCase(Locale.ROOT));
                    statement.setLong(4, at.toEpochMilli());
                    statement.setString(5, sessionId.toString());
                    statement.executeUpdate();
                }
                PlayerProfile profile = this.read(uuid).orElseThrow();
                this.connection.commit();
                return profile;
            } catch (SQLException | RuntimeException failure) {
                this.connection.rollback();
                throw failure;
            } finally {
                this.connection.setAutoCommit(true);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> recordQuit(UUID uuid, UUID sessionId, Instant at) {
        return this.submit(() -> {
            try (var statement = this.connection.prepareStatement("""
                UPDATE player_profiles SET last_logout_at = ?, session_id = NULL
                WHERE uuid = ? AND session_id = ?
                """)) {
                statement.setLong(1, at.toEpochMilli());
                statement.setString(2, uuid.toString());
                statement.setString(3, sessionId.toString());
                return statement.executeUpdate() == 1;
            }
        });
    }

    private Optional<PlayerProfile> read(UUID uuid) throws SQLException {
        try (var statement = this.connection.prepareStatement("SELECT * FROM player_profiles WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(profile(result)) : Optional.empty();
            }
        }
    }

    private static PlayerProfile profile(ResultSet result) throws SQLException {
        long logout = result.getLong("last_logout_at");
        Instant logoutAt = result.wasNull() ? null : Instant.ofEpochMilli(logout);
        String session = result.getString("session_id");
        return new PlayerProfile(
            UUID.fromString(result.getString("uuid")),
            result.getString("last_known_name"),
            Instant.ofEpochMilli(result.getLong("last_login_at")),
            logoutAt,
            session == null ? null : UUID.fromString(session)
        );
    }

    private synchronized <T> CompletableFuture<T> submit(SqlOperation<T> operation) {
        if (this.closing != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Player database is closed"));
        }
        var result = new CompletableFuture<T>();
        this.executor.execute(() -> complete(result, operation));
        return result;
    }

    @Override
    public synchronized CompletableFuture<Void> closeAsync() {
        if (this.closing == null) {
            this.closing = new CompletableFuture<>();
            this.executor.execute(() -> complete(this.closing, () -> {
                this.connection.close();
                return null;
            }));
            this.executor.shutdown();
        }
        return this.closing;
    }

    private static <T> void complete(CompletableFuture<T> future, SqlOperation<T> operation) {
        try {
            future.complete(operation.run());
        } catch (Exception failure) {
            future.completeExceptionally(failure);
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T run() throws SQLException;
    }
}
