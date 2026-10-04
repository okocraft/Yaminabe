package net.okocraft.yaminabe.common.player;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Owns connection tokens. It never retains platform players or publishes a mutable profile cache. */
public final class PlayerProfileService {

    public record Session(UUID playerId, UUID sessionId, CompletableFuture<PlayerProfile> ready) {
    }

    private final PlayerProfileRepository repository;
    private final Clock clock;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private boolean closed;
    private CompletableFuture<Void> closing;

    public PlayerProfileService(PlayerProfileRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized Session join(UUID uuid, String name) {
        if (this.closed) {
            throw new IllegalStateException("Player profile service is closed");
        }
        UUID token = UUID.randomUUID();
        var ready = this.repository.recordJoin(uuid, name, token, this.clock.instant());
        var session = new Session(uuid, token, ready);
        this.sessions.put(uuid, session);
        return session;
    }

    public synchronized CompletableFuture<Boolean> quit(Session session) {
        if (this.closed || !this.sessions.remove(session.playerId(), session)) {
            return CompletableFuture.completedFuture(false);
        }
        return this.repository.recordQuit(session.playerId(), session.sessionId(), this.clock.instant());
    }

    public synchronized boolean isOnline(UUID uuid) {
        return this.sessions.containsKey(uuid);
    }

    public CompletableFuture<Optional<PlayerProfile>> find(UUID uuid) {
        return this.repository.find(uuid);
    }

    public CompletableFuture<List<PlayerProfile>> findByName(String name) {
        return this.repository.findByName(name);
    }

    public synchronized CompletableFuture<Void> closeAsync() {
        if (this.closing != null) {
            return this.closing;
        }
        this.closed = true;
        // Snapshot observations now; the I/O queue must never call back into a platform scheduler.
        var at = this.clock.instant();
        var writes = this.sessions.values().stream()
            .map(session -> this.repository.recordQuit(session.playerId(), session.sessionId(), at))
            .toArray(CompletableFuture[]::new);
        this.sessions.clear();
        var drained = CompletableFuture.allOf(writes);
        var closedStorage = this.repository.closeAsync();
        this.closing = CompletableFuture.allOf(drained, closedStorage);
        return this.closing;
    }
}
