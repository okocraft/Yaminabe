package net.okocraft.yaminabe.common.player;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A stored observation, not an assertion that a player is currently online. */
public record PlayerProfile(
    UUID uuid,
    String lastKnownName,
    Instant lastLoginAt,
    @Nullable Instant lastLogoutAt,
    @Nullable UUID openSessionId
) {
    public PlayerProfile {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(lastKnownName);
        Objects.requireNonNull(lastLoginAt);
    }

    public boolean logoutConfirmed() {
        return this.openSessionId == null && this.lastLogoutAt != null;
    }
}
