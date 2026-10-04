package net.okocraft.yaminabe.common.player;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementations serialize reads and writes in submission order. An empty result means unregistered;
 * storage failures complete exceptionally and must never be replaced with a default profile.
 * Future feature repositories sharing a database must use the same ordered I/O queue.
 */
public interface PlayerProfileRepository {

    CompletableFuture<Optional<PlayerProfile>> find(UUID uuid);

    /** Exact, case-insensitive last-known-name matches. Names are not unique identities. */
    CompletableFuture<List<PlayerProfile>> findByName(String name);

    CompletableFuture<PlayerProfile> recordJoin(UUID uuid, String name, UUID sessionId, Instant at);

    /** A stale session must not close a newer connection. Returns whether the session was closed. */
    CompletableFuture<Boolean> recordQuit(UUID uuid, UUID sessionId, Instant at);

    /** Rejects new work, drains accepted work, then closes the storage resources. Idempotent. */
    CompletableFuture<Void> closeAsync();
}
