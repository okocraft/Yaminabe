package net.okocraft.yaminabe.common.restart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.okocraft.yaminabe.common.command.argument.TokenArgumentType;
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
            .requires(source -> hasAnyPermission(source, sourceAdapter))
            .then(createAction("restart", RestartCommandPermissions.RESTART, ShutdownType.RESTART, executor, sourceAdapter))
            .then(createAction("stop", RestartCommandPermissions.STOP, ShutdownType.STOP, executor, sourceAdapter))
            .then(LiteralArgumentBuilder.<S>literal("cancel")
                .requires(source -> sourceAdapter.hasPermission(source, RestartCommandPermissions.CANCEL))
                .executes(context -> executor.cancel(context.getSource())))
            .build();
    }

    private static <S> boolean hasAnyPermission(S source, RestartCommandSource<S> sourceAdapter) {
        return sourceAdapter.hasPermission(source, RestartCommandPermissions.RESTART)
            || sourceAdapter.hasPermission(source, RestartCommandPermissions.STOP)
            || sourceAdapter.hasPermission(source, RestartCommandPermissions.CANCEL);
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
            .then(createTimedBranch(
                "in",
                "duration",
                StringArgumentType.word(),
                type,
                executor::scheduleIn
            ))
            .then(createTimedBranch(
                "at",
                "date-time",
                TokenArgumentType.token(),
                type,
                executor::scheduleAt
            ));
    }

    private static <S> LiteralArgumentBuilder<S> createNowBranch(
        ShutdownType type,
        RestartCommandExecutor<S> executor
    ) {
        return LiteralArgumentBuilder.<S>literal("now")
            .executes(context -> executor.scheduleNow(context.getSource(), type, null))
            .then(createReasonBranch(context -> executor.scheduleNow(
                context.getSource(),
                type,
                StringArgumentType.getString(context, "reason")
            )));
    }

    private static <S> LiteralArgumentBuilder<S> createTimedBranch(
        String literal,
        String argumentName,
        ArgumentType<String> argumentType,
        ShutdownType type,
        TimedScheduler<S> scheduler
    ) {
        RequiredArgumentBuilder<S, String> argument = RequiredArgumentBuilder.argument(argumentName, argumentType);

        argument.executes(context -> scheduleTimed(context, argumentName, type, scheduler, null, null));
        argument.then(LiteralArgumentBuilder.<S>literal("countdown")
            .then(RequiredArgumentBuilder.<S, String>argument("countdown", StringArgumentType.word())
                .executes(context -> scheduleTimed(
                    context,
                    argumentName,
                    type,
                    scheduler,
                    StringArgumentType.getString(context, "countdown"),
                    null
                ))
                .then(createReasonBranch(context -> scheduleTimed(
                    context,
                    argumentName,
                    type,
                    scheduler,
                    StringArgumentType.getString(context, "countdown"),
                    StringArgumentType.getString(context, "reason")
                )))));
        argument.then(createReasonBranch(context -> scheduleTimed(
            context,
            argumentName,
            type,
            scheduler,
            null,
            StringArgumentType.getString(context, "reason")
        )));

        return LiteralArgumentBuilder.<S>literal(literal).then(argument);
    }

    private static <S> LiteralArgumentBuilder<S> createReasonBranch(Command<S> command) {
        return LiteralArgumentBuilder.<S>literal("reason")
            .then(RequiredArgumentBuilder.<S, String>argument("reason", StringArgumentType.greedyString())
                .executes(command));
    }

    private static <S> int scheduleTimed(
        CommandContext<S> context,
        String argumentName,
        ShutdownType type,
        TimedScheduler<S> scheduler,
        @Nullable String countdown,
        @Nullable String reason
    ) {
        return scheduler.schedule(
            context.getSource(),
            type,
            context.getArgument(argumentName, String.class),
            countdown,
            reason
        );
    }

    @FunctionalInterface
    private interface TimedScheduler<S> {
        int schedule(
            S source,
            ShutdownType type,
            String input,
            @Nullable String countdown,
            @Nullable String reason
        );
    }

    private AutoRestartCommand() {
        throw new UnsupportedOperationException();
    }
}
