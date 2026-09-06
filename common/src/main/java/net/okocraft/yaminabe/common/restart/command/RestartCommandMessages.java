package net.okocraft.yaminabe.common.restart.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import dev.siroshun.mcmsgdef.MessageKey;
import net.kyori.adventure.text.minimessage.translation.Argument;

public final class RestartCommandMessages {

    public static final DefaultMessageDefiner DEFINER = DefaultMessageDefiner.create();
    private static final String PREFIX = "yaminabe.command.autorestart.";

    static final MessageKey.Arg1<String> RESTART_SCHEDULED = DEFINER
        .define(PREFIX + "restart-scheduled", "<gray>Restart scheduled for <aqua><time></aqua><gray>.")
        .with(time -> Argument.string("time", time));
    static final MessageKey.Arg1<String> STOP_SCHEDULED = DEFINER
        .define(PREFIX + "stop-scheduled", "<gray>Shutdown scheduled for <aqua><time></aqua><gray>.")
        .with(time -> Argument.string("time", time));
    static final MessageKey RESTART_IMMEDIATE = DEFINER
        .define(PREFIX + "restart-immediate", "<gray>Restart scheduled immediately.");
    static final MessageKey STOP_IMMEDIATE = DEFINER
        .define(PREFIX + "stop-immediate", "<gray>Shutdown scheduled immediately.");
    static final MessageKey.Arg1<String> RESTART_CANCELLED = DEFINER
        .define(PREFIX + "restart-cancelled", "<gray>Cancelled the restart scheduled for <aqua><time></aqua><gray>.")
        .with(time -> Argument.string("time", time));
    static final MessageKey.Arg1<String> STOP_CANCELLED = DEFINER
        .define(PREFIX + "stop-cancelled", "<gray>Cancelled the shutdown scheduled for <aqua><time></aqua><gray>.")
        .with(time -> Argument.string("time", time));
    static final MessageKey NOTHING_TO_CANCEL = DEFINER
        .define(PREFIX + "nothing-to-cancel", "<red>There is no restart or shutdown reservation to cancel.");
    static final MessageKey.Arg1<String> INVALID_DURATION = DEFINER
        .define(PREFIX + "invalid-duration", "<red>Invalid duration: <aqua><input></aqua><red>.")
        .with(input -> Argument.string("input", input));
    static final MessageKey.Arg1<String> INVALID_DATE_TIME = DEFINER
        .define(PREFIX + "invalid-date-time", "<red>Invalid date or time: <aqua><input></aqua><red>.")
        .with(input -> Argument.string("input", input));
    static final MessageKey SCHEDULE_FAILED = DEFINER
        .define(PREFIX + "schedule-failed", "<red>The reservation could not be activated.");

    private RestartCommandMessages() {
        throw new UnsupportedOperationException();
    }
}
