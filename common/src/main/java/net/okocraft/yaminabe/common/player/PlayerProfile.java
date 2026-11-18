package net.okocraft.yaminabe.common.player;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** The minimal persistent identity known to this server. */
public record PlayerProfile(UUID uuid, String name, Instant updatedAt) {
    public PlayerProfile {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        Objects.requireNonNull(updatedAt);
    }
}
