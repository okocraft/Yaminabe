package net.okocraft.yaminabe.paper.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.JDBC;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SqlitePlayerProfileRepositoryTest {

    @TempDir
    Path directory;
    private static final Instant JOIN = Instant.parse("2026-10-04T11:00:00Z");

    @Test
    void persistsProfilesAndKeepsNamesNonUnique() throws Exception {
        Path file = this.directory.resolve("players.db");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        var repository = SqlitePlayerProfileRepository.open(file);
        assertTrue(await(repository.find(first)).isEmpty());
        var profile = await(repository.recordJoin(first, "Example", session, JOIN));
        assertNull(profile.lastLogoutAt());
        assertFalse(profile.logoutConfirmed());
        assertTrue(await(repository.recordQuit(first, session, JOIN.plusSeconds(30))));
        await(repository.recordJoin(second, "EXAMPLE", UUID.randomUUID(), JOIN));
        assertEquals(2, await(repository.findByName("example")).size());
        await(repository.closeAsync());

        var reopened = SqlitePlayerProfileRepository.open(file);
        try {
            profile = await(reopened.find(first)).orElseThrow();
            assertEquals(JOIN, profile.lastLoginAt());
            assertEquals(JOIN.plusSeconds(30), profile.lastLogoutAt());
            assertTrue(profile.logoutConfirmed());
        } finally {
            await(reopened.closeAsync());
        }
    }

    @Test
    void serializesReadsWithWritesAndRejectsStaleQuit() throws Exception {
        var repository = SqlitePlayerProfileRepository.open(this.directory.resolve("players.db"));
        UUID uuid = UUID.randomUUID();
        UUID oldSession = UUID.randomUUID();
        UUID newSession = UUID.randomUUID();
        var first = repository.recordJoin(uuid, "OldName", oldSession, JOIN);
        var second = repository.recordJoin(uuid, "NewName", newSession, JOIN.minusSeconds(10));
        var staleQuit = repository.recordQuit(uuid, oldSession, JOIN.plusSeconds(30));
        var read = repository.find(uuid);
        await(first);
        await(second);
        assertFalse(await(staleQuit));
        var profile = await(read).orElseThrow();
        assertEquals("NewName", profile.lastKnownName());
        assertEquals(newSession, profile.openSessionId());
        assertFalse(profile.logoutConfirmed()); // Clock order does not determine session closure.
        assertTrue(await(repository.findByName("OldName")).isEmpty());
        await(repository.closeAsync());
    }

    @Test
    void closeDrainsAcceptedWorkAndRejectsNewWork() throws Exception {
        Path file = this.directory.resolve("players.db");
        var repository = SqlitePlayerProfileRepository.open(file);
        UUID uuid = UUID.randomUUID();
        var write = repository.recordJoin(uuid, "Example", UUID.randomUUID(), JOIN);
        var closing = repository.closeAsync();
        assertSame(closing, repository.closeAsync());
        assertThrows(Exception.class, () -> await(repository.find(uuid)));
        await(closing);
        assertEquals(uuid, await(write).uuid());
        var reopened = SqlitePlayerProfileRepository.open(file);
        try {
            // Closing the repository itself must not manufacture a logout after a crash/unclean session.
            assertFalse(await(reopened.find(uuid)).orElseThrow().logoutConfirmed());
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
    void rollsBackFailedMigrationWithoutAdvancingVersion() throws Exception {
        Path file = this.directory.resolve("players.db");
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE existing (name_key TEXT)");
            statement.execute("CREATE INDEX player_profiles_name ON existing(name_key)");
        }
        assertThrows(java.sql.SQLException.class, () -> SqlitePlayerProfileRepository.open(file));
        try (var connection = new JDBC().connect("jdbc:sqlite:" + file, new Properties());
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT name FROM sqlite_master WHERE name = 'player_profiles'")) {
                assertFalse(result.next());
            }
        }
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
