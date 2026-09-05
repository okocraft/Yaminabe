package net.okocraft.yaminabe.common.restart.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

public final class AutoRestartCommand {

    public static <S> LiteralCommandNode<S> create(
        String literal,
        RestartService service,
        Clock clock,
        Supplier<RestartCommandSettings> settingsSupplier,
        RestartCommandSource<S> sourceAdapter
    ) {
        Objects.requireNonNull(literal);
        RestartCommandExecutor<S> executor = new RestartCommandExecutor<>(
            service,
            clock,
            settingsSupplier,
            sourceAdapter
        );

        return LiteralArgumentBuilder.<S>literal(literal)
            .then(createAction("restart", RestartCommandPermissions.RESTART, ShutdownType.RESTART, executor, sourceAdapter))
            .then(createAction("stop", RestartCommandPermissions.STOP, ShutdownType.STOP, executor, sourceAdapter))
            .then(LiteralArgumentBuilder.<S>literal("cancel")
                .requires(source -> sourceAdapter.hasPermission(source, RestartCommandPermissions.CANCEL))
                .executes(context -> executor.cancel(context.getSource())))
            .build();
    }

    private static <S> LiteralArgumentBuilder<S> createAction(
        String literal,
        String permission,
        ShutdownType type,
        RestartCommandExecutor<S> executor,
        RestartCommandSource<S> sourceAdapter
    ) {
        return LiteralArgumentBuilder.<S>literal(literal)
            .requires(source -> sourceAdapter.hasPermission(source, permission))
            .executes(context -> executor.scheduleDefault(context.getSource(), type))
            .then(createNowBranch(type, executor))
            .then(createTimedBranch("in", false, type, executor))
            .then(createTimedBranch("at", true, type, executor));
    }

    private static <S> LiteralArgumentBuilder<S> createNowBranch(
        ShutdownType type,
        RestartCommandExecutor<S> executor
    ) {
        return LiteralArgumentBuilder.<S>literal("now")
            .executes(context -> executor.scheduleNow(context.getSource(), type, null))
            .then(LiteralArgumentBuilder.<S>literal("reason")
                .then(RequiredArgumentBuilder.<S, String>argument("reason", StringArgumentType.greedyString())
                    .executes(context -> executor.scheduleNow(
                        context.getSource(),
                        type,
                        StringArgumentType.getString(context, "reason")
                    ))));
    }

    private static <S> LiteralArgumentBuilder<S> createTimedBranch(
        String literal,
        boolean dateTime,
        ShutdownType type,
        RestartCommandExecutor<S> executor
    ) {
        String argumentName = dateTime ? "date-time" : "duration";
        RequiredArgumentBuilder<S, String> argument = RequiredArgumentBuilder.argument(
            argumentName,
            StringArgumentType.word()
        );

        argument.executes(context -> scheduleTimed(context, dateTime, type, executor, null, null));
        argument.then(LiteralArgumentBuilder.<S>literal("countdown")
            .then(RequiredArgumentBuilder.<S, String>argument("countdown", StringArgumentType.word())
                .executes(context -> scheduleTimed(
                    context,
                    dateTime,
                    type,
                    executor,
                    StringArgumentType.getString(context, "countdown"),
                    null
                ))
                .then(LiteralArgumentBuilder.<S>literal("reason")
                    .then(RequiredArgumentBuilder.<S, String>argument("reason", StringArgumentType.greedyString())
                        .executes(context -> scheduleTimed(
                            context,
                            dateTime,
                            type,
                            executor,
                            StringArgumentType.getString(context, "countdown"),
                            StringArgumentType.getString(context, "reason")
                        ))))));
        argument.then(LiteralArgumentBuilder.<S>literal("reason")
            .then(RequiredArgumentBuilder.<S, String>argument("reason", StringArgumentType.greedyString())
                .executes(context -> scheduleTimed(
                    context,
                    dateTime,
                    type,
                    executor,
                    null,
                    StringArgumentType.getString(context, "reason")
                ))));

        return LiteralArgumentBuilder.<S>literal(literal).then(argument);
    }

    private static <S> int scheduleTimed(
        CommandContext<S> context,
        boolean dateTime,
        ShutdownType type,
        RestartCommandExecutor<S> executor,
        @Nullable String countdown,
        @Nullable String reason
    ) {
        String input = StringArgumentType.getString(context, dateTime ? "date-time" : "duration");
        return dateTime
            ? executor.scheduleAt(context.getSource(), type, input, countdown, reason)
            : executor.scheduleIn(context.getSource(), type, input, countdown, reason);
    }

    private AutoRestartCommand() {
        throw new UnsupportedOperationException();
    }
}
