package net.okocraft.yaminabe.common.restart.countdown;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@NotNullByDefault
public final class RestartCountdownPresenter implements AutoCloseable {

    private static final Duration TICK_INTERVAL = Duration.ofSeconds(1);

    private final Scheduler scheduler;
    private final Clock clock;
    private final AudienceProvider audienceProvider;
    private final Supplier<RestartCountdownSettings> settingsSupplier;
    private final Object stateLock = new Object();
    private @Nullable ActiveCountdown active;

    public RestartCountdownPresenter(
        Scheduler scheduler,
        Clock clock,
        AudienceProvider audienceProvider,
        Supplier<RestartCountdownSettings> settingsSupplier
    ) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.clock = Objects.requireNonNull(clock);
        this.audienceProvider = Objects.requireNonNull(audienceProvider);
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier);
    }

    public void start(ShutdownReservation reservation) {
        Objects.requireNonNull(reservation);
        long remaining = this.remainingSeconds(reservation);
        if (remaining <= 0) {
            return;
        }

        RestartCountdownSettings settings = Objects.requireNonNull(
            this.settingsSupplier.get(),
            "settingsSupplier returned null"
        );
        if (!settings.bossBarEnabled() && settings.broadcastAtSeconds().isEmpty()) {
            return;
        }

        long total = Math.max(1, ceilSeconds(Duration.between(reservation.countdownStartAt(), reservation.executeAt())));
        BossBar bossBar = settings.bossBarEnabled()
            ? BossBar.bossBar(
                RestartCountdownMessages.bossBar(reservation, remaining),
                progress(remaining, total),
                settings.bossBarColor(),
                settings.bossBarOverlay()
            )
            : null;
        ActiveCountdown next = new ActiveCountdown(reservation, settings, total, remaining, bossBar);
        ActiveCountdown previous;
        synchronized (this.stateLock) {
            previous = this.active;
            this.active = next;
        }
        this.closeActive(previous);
        this.render(next, remaining);

        CancellableTask task = this.scheduler.runAtFixedRate(
            scheduledTask -> this.tick(next, scheduledTask),
            TICK_INTERVAL
        );
        synchronized (this.stateLock) {
            if (this.active == next) {
                next.task = task;
            } else {
                task.cancel();
            }
        }
    }

    public void stop(ShutdownReservation reservation) {
        Objects.requireNonNull(reservation);
        ActiveCountdown active;
        synchronized (this.stateLock) {
            active = this.active;
            if (active == null || !active.reservation.equals(reservation)) {
                return;
            }
            this.active = null;
        }
        this.closeActive(active);
    }

    public void showTo(Audience audience) {
        Objects.requireNonNull(audience);
        synchronized (this.stateLock) {
            ActiveCountdown active = this.active;
            if (active != null && active.bossBar != null && active.bossBarShown) {
                audience.showBossBar(active.bossBar);
            }
        }
    }

    @Override
    public void close() {
        ActiveCountdown active;
        synchronized (this.stateLock) {
            active = this.active;
            this.active = null;
        }
        this.closeActive(active);
    }

    private void tick(ActiveCountdown countdown, CancellableTask scheduledTask) {
        synchronized (this.stateLock) {
            if (this.active != countdown) {
                scheduledTask.cancel();
                return;
            }
        }

        long remaining = this.remainingSeconds(countdown.reservation);
        if (remaining <= 0) {
            synchronized (this.stateLock) {
                if (this.active == countdown) {
                    this.active = null;
                }
            }
            scheduledTask.cancel();
            this.closeActive(countdown);
            return;
        }
        this.render(countdown, remaining);
    }

    private void render(ActiveCountdown countdown, long remaining) {
        synchronized (this.stateLock) {
            if (this.active != countdown) {
                return;
            }

            if (countdown.bossBar != null) {
                countdown.bossBar
                    .name(RestartCountdownMessages.bossBar(countdown.reservation, remaining))
                    .progress(progress(remaining, countdown.totalSeconds));
                if (!countdown.bossBarShown) {
                    countdown.bossBarShown = true;
                    for (Audience audience : this.audienceProvider.audiences()) {
                        audience.showBossBar(countdown.bossBar);
                    }
                }
            }

            if (remaining <= countdown.lastRemainingSeconds) {
                for (long threshold : countdown.settings.broadcastAtSeconds()) {
                    if (threshold <= countdown.lastRemainingSeconds
                        && threshold >= remaining
                        && countdown.broadcasted.add(threshold)) {
                        ComponentLike message = RestartCountdownMessages.countdown(countdown.reservation, threshold);
                        for (Audience audience : this.audienceProvider.audiences()) {
                            audience.sendMessage(message.asComponent());
                        }
                    }
                }
            }
            countdown.lastRemainingSeconds = remaining;
        }
    }

    private long remainingSeconds(ShutdownReservation reservation) {
        return Math.max(0, ceilSeconds(Duration.between(this.clock.instant(), reservation.executeAt())));
    }

    private void closeActive(@Nullable ActiveCountdown countdown) {
        if (countdown == null) {
            return;
        }

        @Nullable CancellableTask task;
        boolean hideBossBar;
        synchronized (this.stateLock) {
            task = countdown.task;
            countdown.task = null;
            hideBossBar = countdown.bossBar != null && countdown.bossBarShown;
            countdown.bossBarShown = false;
        }

        if (task != null) {
            task.cancel();
        }
        if (hideBossBar) {
            for (Audience audience : this.audienceProvider.audiences()) {
                audience.hideBossBar(countdown.bossBar);
            }
        }
    }

    private static long ceilSeconds(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return 0;
        }
        long seconds = duration.getSeconds();
        return duration.getNano() == 0 ? seconds : Math.addExact(seconds, 1);
    }

    private static float progress(long remaining, long total) {
        return Math.max(0.0F, Math.min(1.0F, (float) remaining / (float) total));
    }

    @FunctionalInterface
    public interface AudienceProvider {
        Collection<? extends Audience> audiences();
    }

    private static final class ActiveCountdown {
        private final ShutdownReservation reservation;
        private final RestartCountdownSettings settings;
        private final long totalSeconds;
        private final @Nullable BossBar bossBar;
        private final Set<Long> broadcasted = new HashSet<>();
        private @Nullable CancellableTask task;
        private long lastRemainingSeconds;
        private boolean bossBarShown;

        private ActiveCountdown(
            ShutdownReservation reservation,
            RestartCountdownSettings settings,
            long totalSeconds,
            long initialRemainingSeconds,
            @Nullable BossBar bossBar
        ) {
            this.reservation = reservation;
            this.settings = settings;
            this.totalSeconds = totalSeconds;
            this.lastRemainingSeconds = initialRemainingSeconds;
            this.bossBar = bossBar;
        }
    }
}
