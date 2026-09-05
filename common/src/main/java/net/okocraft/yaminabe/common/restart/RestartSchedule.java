package net.okocraft.yaminabe.common.restart;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

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
        LocalDate date = instant.atZone(this.zoneId).toLocalDate();
        return IntStream.rangeClosed(0, 2)
            .mapToObj(dayOffset -> date.plusDays(dayOffset))
            .flatMap(candidateDate -> this.times.stream().map(time -> LocalDateTime.of(candidateDate, time)))
            .map(dateTime -> RestartDateTimeParser.resolveFuture(dateTime, instant, this.zoneId))
            .flatMap(Optional::stream)
            .min(Instant::compareTo);
    }
}
