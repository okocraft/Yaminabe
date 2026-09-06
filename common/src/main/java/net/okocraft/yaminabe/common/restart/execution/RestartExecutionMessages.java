package net.okocraft.yaminabe.common.restart.execution;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;

import java.util.Objects;

public final class RestartExecutionMessages {

    public static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();
    private static final String PREFIX = "yaminabe.restart.";

    private static final MessageKey RESTART_KICK = DEFINER
        .define(PREFIX + "restart-kick", "<red>The server is restarting.</red>");
    private static final MessageKey STOP_KICK = DEFINER
        .define(PREFIX + "stop-kick", "<red>The server is shutting down.</red>");
    private static final MessageKey.Arg1<String> RESTART_KICK_WITH_REASON = DEFINER
        .define(PREFIX + "restart-kick-with-reason", "<red>The server is restarting: <reason></red>")
        .with(reason -> Argument.string("reason", reason));
    private static final MessageKey.Arg1<String> STOP_KICK_WITH_REASON = DEFINER
        .define(PREFIX + "stop-kick-with-reason", "<red>The server is shutting down: <reason></red>")
        .with(reason -> Argument.string("reason", reason));

    public static ComponentLike kickMessage(ShutdownReservation reservation) {
        Objects.requireNonNull(reservation);
        String reason = reservation.reason();
        if (reason == null) {
            return reservation.type() == ShutdownType.RESTART ? RESTART_KICK : STOP_KICK;
        }
        return reservation.type() == ShutdownType.RESTART
            ? RESTART_KICK_WITH_REASON.apply(reason)
            : STOP_KICK_WITH_REASON.apply(reason);
    }

    private RestartExecutionMessages() {
        throw new UnsupportedOperationException();
    }
}
