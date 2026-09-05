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
            .then(createInBranch(type, executor))
            .then(createAtBranch(type, executor));
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

    private static <S> LiteralArgumentBuilder<S> createInBranch(
        ShutdownType type,
        RestartCommandExecutor<S> executor
    ) {
        RequiredArgumentBuilder<S, String> duration = RequiredArgumentBuilder.argument(
            "duration",
            StringArgumentType.word()
        );

        duration.executes(context -> scheduleIn(context, type, executor, null, null));
        duration.then(LiteralArgumentBuilder.<S>literal("countdown")
            .then(RequiredArgumentBuilder.<S, String>argument("countdown", StringArgumentType.word())
                .executes(context -> scheduleIn(
                    context,
                    type,
                    executor,
                    StringArgumentType.getString(context, "countdown"),
                    null
                ))
                .then(LiteralArgumentBuilder.<S>literal("reason")
                    .then(RequiredArgumentBuilder.<S, String>argument("reason", StringArgumentType.greedyString())
                        .executes(context -> scheduleIn(
                            context,
                            type,
                            executor,
                            StringArgumentType.getString(context, "countdown"),
                            StringArgumentType.getString(context, "reason")
                        ))))));
        duration.then(LiteralArgumentBuilder.<S>literal("reason")
            .then(RequiredArgumentBuilder.<S, String>argument("reason", StringArgumentType.greedyString())
                .executes(context -> scheduleIn(
                    context,
                    type,
                    executor,
                    null,
                    StringArgumentType.getString(context, "reason")
                ))));

        return LiteralArgumentBuilder.<S>literal("in").then(duration);
    }

    private static <S> LiteralArgumentBuilder<S> createAtBranch(
        ShutdownType type,
        RestartCommandExecutor<S> executor
    ) {
        return LiteralArgumentBuilder.<S>literal("at")
            .then(RequiredArgumentBuilder.<S, String>argument("at-arguments", StringArgumentType.greedyString())
                .executes(context -> scheduleAt(
                    context.getSource(),
                    type,
                    executor,
                    StringArgumentType.getString(context, "at-arguments")
                )));
    }

    private static <S> int scheduleIn(
        CommandContext<S> context,
        ShutdownType type,
        RestartCommandExecutor<S> executor,
        @Nullable String countdown,
        @Nullable String reason
    ) {
        return executor.scheduleIn(
            context.getSource(),
            type,
            StringArgumentType.getString(context, "duration"),
            countdown,
            reason
        );
    }

    private static <S> int scheduleAt(
        S source,
        ShutdownType type,
        RestartCommandExecutor<S> executor,
        String input
    ) {
        AtArguments arguments = parseAtArguments(input);
        return arguments == null
            ? executor.invalidArguments(source)
            : executor.scheduleAt(source, type, arguments.dateTime(), arguments.countdown(), arguments.reason());
    }

    private static @Nullable AtArguments parseAtArguments(String input) {
        Token dateTime = takeToken(input);
        if (dateTime == null) {
            return null;
        }

        Token option = takeToken(dateTime.remaining());
        if (option == null) {
            return new AtArguments(dateTime.value(), null, null);
        }

        if (option.value().equalsIgnoreCase("reason")) {
            String reason = option.remaining().strip();
            return reason.isEmpty() ? null : new AtArguments(dateTime.value(), null, reason);
        }

        if (!option.value().equalsIgnoreCase("countdown")) {
            return null;
        }

        Token countdown = takeToken(option.remaining());
        if (countdown == null) {
            return null;
        }
        Token reasonLiteral = takeToken(countdown.remaining());
        if (reasonLiteral == null) {
            return new AtArguments(dateTime.value(), countdown.value(), null);
        }
        if (!reasonLiteral.value().equalsIgnoreCase("reason")) {
            return null;
        }
        String reason = reasonLiteral.remaining().strip();
        return reason.isEmpty() ? null : new AtArguments(dateTime.value(), countdown.value(), reason);
    }

    private static @Nullable Token takeToken(String input) {
        String value = input.stripLeading();
        if (value.isEmpty()) {
            return null;
        }
        int index = 0;
        while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return new Token(value.substring(0, index), value.substring(index));
    }

    private record AtArguments(String dateTime, @Nullable String countdown, @Nullable String reason) {
    }

    private record Token(String value, String remaining) {
    }

    private AutoRestartCommand() {
        throw new UnsupportedOperationException();
    }
}
