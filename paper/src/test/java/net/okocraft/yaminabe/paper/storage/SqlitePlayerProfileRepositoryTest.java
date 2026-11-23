package net.okocraft.yaminabe.paper.storage;

import net.okocraft.yaminabe.common.player.PlayerProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.JDBC;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SqlitePlayerProfileRepositoryTest {

    @TempDir
    Path directory;
    private static final Instant UPDATED = Instant.parse("2026-10-05T00:00:00Z");

    @Test
    void persistsExactlyTheThreeIdentityFields() throws Exception {
        Path file = this.directory.resolve("players.db");
        var repository = SqlitePlayerProfileRepository.open(file);
        var profile = new PlayerProfile(UUID.randomUUID(), "Example", UPDATED);
        assertTrue(await(repository.find(profile.uuid())).isEmpty());
        await(repository.upsert(profile));
        await(repository.closeAsync());

        var reopened = SqlitePlayerProfileRepository.open(file);
        try {
            assertEquals(profile, await(reopened.find(profile.uuid())).orElseThrow());
        } finally {
            await(reopened.closeAsync());
        }
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA table_info(player_profiles)")) {
            var columns = new ArrayList<String>();
            while (result.next()) {
                columns.add(result.getString("name"));
            }
            assertEquals(List.of("uuid", "name", "updated_at"), columns);
        }
    }

    @Test
    void ordersUpsertsAndReadsWithoutTreatingNamesAsUnique() throws Exception {
        var repository = SqlitePlayerProfileRepository.open(this.directory.resolve("players.db"));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var initial = repository.upsert(new PlayerProfile(first, "OldName", UPDATED));
        var renamed = new PlayerProfile(first, "Example", UPDATED.plusSeconds(20));
        var update = repository.upsert(renamed);
        var sameName = new PlayerProfile(second, "Example", UPDATED.plusSeconds(30));
        var otherUpdate = repository.upsert(sameName);
        var read = repository.find(first);
        await(initial);
        await(update);
        await(otherUpdate);
        assertEquals(renamed, await(read).orElseThrow());
        assertEquals(sameName, await(repository.find(second)).orElseThrow());
        await(repository.closeAsync());
    }

    @Test
    void closeDrainsAcceptedWorkAndRejectsNewWork() throws Exception {
        Path file = this.directory.resolve("players.db");
        var repository = SqlitePlayerProfileRepository.open(file);
        var profile = new PlayerProfile(UUID.randomUUID(), "Example", UPDATED);
        var write = repository.upsert(profile);
        var closing = repository.closeAsync();
        assertSame(closing, repository.closeAsync());
        assertThrows(java.util.concurrent.ExecutionException.class, () -> await(repository.find(profile.uuid())));
        assertThrows(java.util.concurrent.ExecutionException.class, () -> await(repository.upsert(profile)));
        await(closing);
        await(write);
        var reopened = SqlitePlayerProfileRepository.open(file);
        try {
            assertEquals(profile, await(reopened.find(profile.uuid())).orElseThrow());
        } finally {
            await(reopened.closeAsync());
        }
    }

    @Test
    void preservesUnknownSchemaAndRefusesToStart() throws Exception {
        Path file = this.directory.resolve("players.db");
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = 42");
            statement.execute("CREATE TABLE valuable_data (value TEXT)");
            statement.execute("INSERT INTO valuable_data VALUES ('keep')");
        }
        assertThrows(java.sql.SQLException.class, () -> SqlitePlayerProfileRepository.open(file));
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT value FROM valuable_data")) {
            assertTrue(result.next());
            assertEquals("keep", result.getString(1));
        }
    }

    @Test
    void initializationFailurePreservesExistingDataAndVersion() throws Exception {
        Path file = this.directory.resolve("players.db");
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE player_profiles (value TEXT)");
            statement.execute("INSERT INTO player_profiles VALUES ('keep')");
        }
        assertThrows(java.sql.SQLException.class, () -> SqlitePlayerProfileRepository.open(file));
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT value FROM player_profiles")) {
                assertTrue(result.next());
                assertEquals("keep", result.getString(1));
            }
        }
    }

    @Test
    void failedWriteDoesNotReplaceTheStoredIdentity() throws Exception {
        Path file = this.directory.resolve("players.db");
        var repository = SqlitePlayerProfileRepository.open(file);
        var profile = new PlayerProfile(UUID.randomUUID(), "Example", UPDATED);
        await(repository.upsert(profile));
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_update BEFORE UPDATE ON player_profiles BEGIN SELECT RAISE(ABORT, 'rejected'); END");
        }
        assertThrows(java.util.concurrent.ExecutionException.class,
            () -> await(repository.upsert(new PlayerProfile(profile.uuid(), "NewName", UPDATED.plusSeconds(20)))));
        assertEquals(profile, await(repository.find(profile.uuid())).orElseThrow());
        await(repository.closeAsync());
    }

    @Test
    void readFailureIsNotAnUnregisteredPlayer() throws Exception {
        Path file = this.directory.resolve("players.db");
        var repository = SqlitePlayerProfileRepository.open(file);
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE player_profiles");
        }
        assertThrows(java.util.concurrent.ExecutionException.class, () -> await(repository.find(UUID.randomUUID())));
        await(repository.closeAsync());
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }
}
