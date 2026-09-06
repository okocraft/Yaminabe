package net.okocraft.yaminabe.common.restart;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@NotNullByDefault
public final class AutomaticRestartManager implements AutoCloseable {

    private final RestartService service;
    private final Clock clock;
    private final Supplier<Optional<Settings>> settingsSupplier;
    private volatile boolean closed;

    public AutomaticRestartManager(
        RestartService service,
        Clock clock,
        Supplier<Optional<Settings>> settingsSupplier
    ) {
        this.service = Objects.requireNonNull(service);
        this.clock = Objects.requireNonNull(clock);
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier);
    }

    public void refresh() {
        if (this.closed) {
            return;
        }

        Optional<Settings> settings = this.settings();
        Optional<RestartService.Snapshot> current = this.service.current();
        if (current.isPresent() && current.get().reservation().source() == ReservationSource.MANUAL) {
            return;
        }

        Instant now = this.clock.instant();
        Optional<ShutdownReservation> next = settings.flatMap(value -> this.createReservation(value, now, now));
        if (next.isPresent()) {
            if (current.isPresent() && current.get().reservation().executeAt().equals(next.get().executeAt())) {
                return;
            }
            this.service.schedule(next.get());
        } else if (current.isPresent()) {
            this.service.cancel();
        }
    }

    public void onCancelled(ShutdownReservation cancelled) {
        Objects.requireNonNull(cancelled);
        if (this.closed || this.service.current().isPresent()) {
            return;
        }

        Instant now = this.clock.instant();
        Instant after = cancelled.source() == ReservationSource.AUTOMATIC && cancelled.executeAt().isAfter(now)
            ? cancelled.executeAt()
            : now;
        this.settings()
            .flatMap(settings -> this.createReservation(settings, now, after))
            .ifPresent(this.service::schedule);
    }

    @Override
    public void close() {
        this.closed = true;
    }

    private Optional<ShutdownReservation> createReservation(Settings settings, Instant createdAt, Instant after) {
        return settings.schedule().nextAfter(after).map(executeAt -> ShutdownReservation.create(
            createdAt,
            executeAt,
            settings.countdown(),
            ShutdownType.RESTART,
            ReservationSource.AUTOMATIC,
            null
        ));
    }

    private Optional<Settings> settings() {
        return Objects.requireNonNull(this.settingsSupplier.get(), "settingsSupplier returned null");
    }

    public record Settings(RestartSchedule schedule, Duration countdown) {

        public Settings {
            Objects.requireNonNull(schedule);
            Objects.requireNonNull(countdown);
            if (countdown.isNegative()) {
                throw new IllegalArgumentException("countdown cannot be negative");
            }
        }
    }
}
