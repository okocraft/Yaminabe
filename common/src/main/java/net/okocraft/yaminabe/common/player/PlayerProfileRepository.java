package net.okocraft.yaminabe.common.player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Reads and writes are serialized in submission order. An empty result means unregistered;
 * storage failures complete exceptionally and must never be replaced with a default profile.
 */
public interface PlayerProfileRepository {

    CompletableFuture<Optional<PlayerProfile>> find(UUID uuid);

    /** Inserts or updates only the specified UUID. Completes after commit. */
    CompletableFuture<Void> upsert(PlayerProfile profile);

    /** Rejects new work, drains accepted work, then closes storage. Idempotent. */
    CompletableFuture<Void> closeAsync();
}
