package net.okocraft.yaminabe.common.restart.command;

import net.okocraft.yaminabe.common.restart.ReservationSource;
import net.okocraft.yaminabe.common.restart.RestartDateTimeParser;
import net.okocraft.yaminabe.common.restart.RestartDurationParser;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Supplier;

final class RestartCommandExecutor<S> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final RestartService service;
    private final Clock clock;
    private final Supplier<RestartCommandSettings> settingsSupplier;
    private final RestartCommandSource<S> sourceAdapter;

    RestartCommandExecutor(
        RestartService service,
        Clock clock,
        Supplier<RestartCommandSettings> settingsSupplier,
        RestartCommandSource<S> sourceAdapter
    ) {
        this.service = Objects.requireNonNull(service);
        this.clock = Objects.requireNonNull(clock);
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier);
        this.sourceAdapter = Objects.requireNonNull(sourceAdapter);
    }

    int scheduleDefault(S source, ShutdownType type) {
        RestartCommandSettings settings = this.settings();
        Instant now = this.clock.instant();
        Duration countdown = settings.defaultCountdown();
        Instant executeAt;
        try {
            executeAt = now.plus(countdown);
        } catch (DateTimeException | ArithmeticException exception) {
            this.sourceAdapter.sendMessage(source, RestartCommandMessages.INVALID_DURATION.apply(countdown.toString()));
            return 0;
        }
        return this.schedule(source, ShutdownReservation.create(
            now,
            executeAt,
            countdown,
            type,
            ReservationSource.MANUAL,
            null
        ), settings);
    }

    int scheduleNow(S source, ShutdownType type, @Nullable String reason) {
        RestartCommandSettings settings = this.settings();
        Instant now = this.clock.instant();
        return this.schedule(source, ShutdownReservation.create(
            now,
            now,
            Duration.ZERO,
            type,
            ReservationSource.MANUAL,
            normalizeReason(reason)
        ), settings);
    }

    int scheduleIn(
        S source,
        ShutdownType type,
        String durationInput,
        @Nullable String countdownInput,
        @Nullable String reason
    ) {
        RestartCommandSettings settings = this.settings();
        Duration delay;
        try {
            delay = RestartDurationParser.parse(durationInput);
        } catch (IllegalArgumentException exception) {
            this.sourceAdapter.sendMessage(source, RestartCommandMessages.INVALID_DURATION.apply(durationInput));
            return 0;
        }

        Instant now = this.clock.instant();
        Instant executeAt;
        try {
            executeAt = now.plus(delay);
        } catch (DateTimeException | ArithmeticException exception) {
            this.sourceAdapter.sendMessage(source, RestartCommandMessages.INVALID_DURATION.apply(durationInput));
            return 0;
        }
        return this.scheduleTimed(source, type, now, executeAt, countdownInput, reason, settings);
    }

    int scheduleAt(
        S source,
        ShutdownType type,
        String dateTimeInput,
        @Nullable String countdownInput,
        @Nullable String reason
    ) {
        RestartCommandSettings settings = this.settings();
        Instant now = this.clock.instant();
        Instant executeAt;
        try {
            executeAt = RestartDateTimeParser.parseFuture(dateTimeInput, now, settings.zoneId());
        } catch (IllegalArgumentException exception) {
            this.sourceAdapter.sendMessage(source, RestartCommandMessages.INVALID_DATE_TIME.apply(dateTimeInput));
            return 0;
        }
        return this.scheduleTimed(source, type, now, executeAt, countdownInput, reason, settings);
    }

    int cancel(S source) {
        RestartCommandSettings settings = this.settings();
        return this.service.cancel()
            .map(reservation -> {
                String time = this.format(reservation.executeAt(), settings);
                this.sourceAdapter.sendMessage(source, switch (reservation.type()) {
                    case RESTART -> RestartCommandMessages.RESTART_CANCELLED.apply(time);
                    case STOP -> RestartCommandMessages.STOP_CANCELLED.apply(time);
                });
                return 1;
            })
            .orElseGet(() -> {
                this.sourceAdapter.sendMessage(source, RestartCommandMessages.NOTHING_TO_CANCEL);
                return 0;
            });
    }

    private int scheduleTimed(
        S source,
        ShutdownType type,
        Instant now,
        Instant executeAt,
        @Nullable String countdownInput,
        @Nullable String reason,
        RestartCommandSettings settings
    ) {
        ShutdownReservation reservation;
        if (countdownInput != null && countdownInput.equalsIgnoreCase("full")) {
            reservation = ShutdownReservation.withFullCountdown(
                now,
                executeAt,
                type,
                ReservationSource.MANUAL,
                normalizeReason(reason)
            );
        } else {
            Duration countdown = settings.defaultCountdown();
            if (countdownInput != null) {
                try {
                    countdown = RestartDurationParser.parse(countdownInput);
                } catch (IllegalArgumentException exception) {
                    this.sourceAdapter.sendMessage(source, RestartCommandMessages.INVALID_DURATION.apply(countdownInput));
                    return 0;
                }
            }
            reservation = ShutdownReservation.create(
                now,
                executeAt,
                countdown,
                type,
                ReservationSource.MANUAL,
                normalizeReason(reason)
            );
        }
        return this.schedule(source, reservation, settings);
    }

    private int schedule(S source, ShutdownReservation reservation, RestartCommandSettings settings) {
        RestartService.ScheduleResult result = this.service.schedule(reservation);
        if (!result.scheduled()) {
            this.sourceAdapter.sendMessage(source, RestartCommandMessages.SCHEDULE_FAILED);
            return 0;
        }

        if (reservation.executeAt().equals(reservation.createdAt())) {
            this.sourceAdapter.sendMessage(source, switch (reservation.type()) {
                case RESTART -> RestartCommandMessages.RESTART_IMMEDIATE;
                case STOP -> RestartCommandMessages.STOP_IMMEDIATE;
            });
        } else {
            String time = this.format(reservation.executeAt(), settings);
            this.sourceAdapter.sendMessage(source, switch (reservation.type()) {
                case RESTART -> RestartCommandMessages.RESTART_SCHEDULED.apply(time);
                case STOP -> RestartCommandMessages.STOP_SCHEDULED.apply(time);
            });
        }
        return 1;
    }

    private String format(Instant instant, RestartCommandSettings settings) {
        return TIME_FORMATTER.withZone(settings.zoneId()).format(instant);
    }

    private RestartCommandSettings settings() {
        return Objects.requireNonNull(this.settingsSupplier.get(), "settingsSupplier returned null");
    }

    private static @Nullable String normalizeReason(@Nullable String reason) {
        return reason == null || reason.isBlank() ? null : reason;
    }
}
