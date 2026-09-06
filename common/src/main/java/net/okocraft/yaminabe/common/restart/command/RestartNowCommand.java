package net.okocraft.yaminabe.common.restart.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownType;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

public final class RestartNowCommand {

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
            .requires(source -> sourceAdapter.hasPermission(source, RestartCommandPermissions.RESTART))
            .executes(context -> executor.scheduleNow(context.getSource(), ShutdownType.RESTART, null))
            .then(LiteralArgumentBuilder.<S>literal("reason")
                .then(RequiredArgumentBuilder.<S, String>argument("reason", StringArgumentType.greedyString())
                    .executes(context -> executor.scheduleNow(
                        context.getSource(),
                        ShutdownType.RESTART,
                        StringArgumentType.getString(context, "reason")
                    ))))
            .build();
    }

    private RestartNowCommand() {
        throw new UnsupportedOperationException();
    }
}
