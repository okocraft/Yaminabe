package net.okocraft.yaminabe.common.restart.countdown;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;

import java.util.Objects;

public final class RestartCountdownMessages {

    public static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();
    private static final String PREFIX = "yaminabe.restart.";

    private static final MessageKey.Arg1<Long> RESTART_COUNTDOWN = DEFINER
        .define(PREFIX + "countdown-restart", "<red>The server will restart in <aqua><seconds></aqua><red> seconds.</red>")
        .with(seconds -> Argument.numeric("seconds", seconds));
    private static final MessageKey.Arg2<Long, String> RESTART_COUNTDOWN_WITH_REASON = DEFINER
        .define(PREFIX + "countdown-restart-with-reason", "<red>The server will restart in <aqua><seconds></aqua><red> seconds: <reason></red>")
        .with(seconds -> Argument.numeric("seconds", seconds), reason -> Argument.string("reason", reason));
    private static final MessageKey.Arg1<Long> STOP_COUNTDOWN = DEFINER
        .define(PREFIX + "countdown-stop", "<red>The server will shut down in <aqua><seconds></aqua><red> seconds.</red>")
        .with(seconds -> Argument.numeric("seconds", seconds));
    private static final MessageKey.Arg2<Long, String> STOP_COUNTDOWN_WITH_REASON = DEFINER
        .define(PREFIX + "countdown-stop-with-reason", "<red>The server will shut down in <aqua><seconds></aqua><red> seconds: <reason></red>")
        .with(seconds -> Argument.numeric("seconds", seconds), reason -> Argument.string("reason", reason));
    private static final MessageKey.Arg1<Long> RESTART_BOSS_BAR = DEFINER
        .define(PREFIX + "boss-bar-restart", "<red>Server restart in <aqua><seconds></aqua><red> seconds</red>")
        .with(seconds -> Argument.numeric("seconds", seconds));
    private static final MessageKey.Arg2<Long, String> RESTART_BOSS_BAR_WITH_REASON = DEFINER
        .define(PREFIX + "boss-bar-restart-with-reason", "<red>Server restart in <aqua><seconds></aqua><red> seconds: <reason></red>")
        .with(seconds -> Argument.numeric("seconds", seconds), reason -> Argument.string("reason", reason));
    private static final MessageKey.Arg1<Long> STOP_BOSS_BAR = DEFINER
        .define(PREFIX + "boss-bar-stop", "<red>Server shutdown in <aqua><seconds></aqua><red> seconds</red>")
        .with(seconds -> Argument.numeric("seconds", seconds));
    private static final MessageKey.Arg2<Long, String> STOP_BOSS_BAR_WITH_REASON = DEFINER
        .define(PREFIX + "boss-bar-stop-with-reason", "<red>Server shutdown in <aqua><seconds></aqua><red> seconds: <reason></red>")
        .with(seconds -> Argument.numeric("seconds", seconds), reason -> Argument.string("reason", reason));

    static ComponentLike countdown(ShutdownReservation reservation, long seconds) {
        Objects.requireNonNull(reservation);
        String reason = reservation.reason();
        if (reservation.type() == ShutdownType.RESTART) {
            return reason == null ? RESTART_COUNTDOWN.apply(seconds) : RESTART_COUNTDOWN_WITH_REASON.apply(seconds, reason);
        }
        return reason == null ? STOP_COUNTDOWN.apply(seconds) : STOP_COUNTDOWN_WITH_REASON.apply(seconds, reason);
    }

    static ComponentLike bossBar(ShutdownReservation reservation, long seconds) {
        Objects.requireNonNull(reservation);
        String reason = reservation.reason();
        if (reservation.type() == ShutdownType.RESTART) {
            return reason == null ? RESTART_BOSS_BAR.apply(seconds) : RESTART_BOSS_BAR_WITH_REASON.apply(seconds, reason);
        }
        return reason == null ? STOP_BOSS_BAR.apply(seconds) : STOP_BOSS_BAR_WITH_REASON.apply(seconds, reason);
    }

    private RestartCountdownMessages() {
        throw new UnsupportedOperationException();
    }
}
