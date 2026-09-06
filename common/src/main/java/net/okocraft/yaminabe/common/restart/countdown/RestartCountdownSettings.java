package net.okocraft.yaminabe.common.restart.countdown;

import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;
import java.util.Set;

@NotNullByDefault
public record RestartCountdownSettings(
    boolean bossBarEnabled,
    BossBar.Color bossBarColor,
    BossBar.Overlay bossBarOverlay,
    Set<Long> broadcastAtSeconds
) {

    public RestartCountdownSettings {
        Objects.requireNonNull(bossBarColor);
        Objects.requireNonNull(bossBarOverlay);
        Objects.requireNonNull(broadcastAtSeconds);
        broadcastAtSeconds = broadcastAtSeconds.stream()
            .map(Objects::requireNonNull)
            .filter(seconds -> seconds > 0)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
