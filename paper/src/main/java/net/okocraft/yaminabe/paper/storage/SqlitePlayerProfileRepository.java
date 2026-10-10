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
                statement.executeQuery("SELECT uuid, name, updated_at FROM player_profiles LIMIT 0").close();
                return;
            }
            connection.setAutoCommit(false);
            try {
                statement.execute("""
                    CREATE TABLE player_profiles (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
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
    public CompletableFuture<Void> upsert(PlayerProfile profile) {
        return this.submit(() -> {
            try (var statement = this.connection.prepareStatement("""
                INSERT INTO player_profiles(uuid, name, updated_at) VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, updated_at = excluded.updated_at
                """)) {
                statement.setString(1, profile.uuid().toString());
                statement.setString(2, profile.name());
                statement.setLong(3, profile.updatedAt().toEpochMilli());
                statement.executeUpdate();
                return null;
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
        return new PlayerProfile(
            UUID.fromString(result.getString("uuid")),
            result.getString("name"),
            Instant.ofEpochMilli(result.getLong("updated_at"))
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
