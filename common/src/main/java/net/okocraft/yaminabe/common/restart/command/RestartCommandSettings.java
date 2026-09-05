package net.okocraft.yaminabe.common.restart.command;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Objects;

public record RestartCommandSettings(Duration defaultCountdown, ZoneId zoneId) {

    public RestartCommandSettings {
        Objects.requireNonNull(defaultCountdown);
        Objects.requireNonNull(zoneId);
        if (defaultCountdown.isNegative()) {
            throw new IllegalArgumentException("defaultCountdown cannot be negative");
        }
    }
}
