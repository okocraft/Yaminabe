package net.okocraft.yaminabe.common.restart;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RestartSchedule(ZoneId zoneId, List<LocalTime> times) {

    public RestartSchedule {
        Objects.requireNonNull(zoneId);
        Objects.requireNonNull(times);
        times = times.stream()
            .map(Objects::requireNonNull)
            .distinct()
            .sorted()
            .toList();
    }

    public RestartSchedule(ZoneId zoneId, Collection<LocalTime> times) {
        this(zoneId, List.copyOf(times));
    }

    public Optional<Instant> nextAfter(Instant instant) {
        Objects.requireNonNull(instant);
        if (this.times.isEmpty()) {
            return Optional.empty();
        }

        LocalDate date = instant.atZone(this.zoneId).toLocalDate();
        for (int dayOffset = 0; dayOffset <= 2; dayOffset++) {
            LocalDate candidateDate = date.plusDays(dayOffset);
            for (LocalTime time : this.times) {
                Instant candidate = ZonedDateTime.of(candidateDate, time, this.zoneId).toInstant();
                if (candidate.isAfter(instant)) {
                    return Optional.of(candidate);
                }
            }
        }
        throw new IllegalStateException("failed to calculate the next scheduled restart");
    }
}
