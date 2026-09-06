package net.okocraft.yaminabe.paper.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.command.AutoRestartCommand;
import net.okocraft.yaminabe.common.restart.command.RestartCommandMessages;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSource;
import net.okocraft.yaminabe.common.restart.command.RestartNowCommand;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownMessages;
import net.okocraft.yaminabe.common.restart.execution.RestartExecutionMessages;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import net.okocraft.yaminabe.paper.platform.RegionScheduler;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;

@NotNullByDefault
public final class YaminabeCommands {

    private static final RestartCommandSource<CommandSourceStack> RESTART_SOURCE = new RestartCommandSource<>() {
        @Override
        public boolean hasPermission(CommandSourceStack source, String permission) {
            return source.getSender().hasPermission(permission);
        }

        @Override
        public void sendMessage(CommandSourceStack source, ComponentLike message) {
            source.getSender().sendMessage(message.asComponent());
        }
    };

    public static List<DefaultMessageDefiner> getDefiners() {
        return List.of(
            CommandMessages.DEFINER,
            RestartCommandMessages.DEFINER,
            RestartExecutionMessages.DEFINER,
            RestartCountdownMessages.DEFINER
        );
    }

    public static void register(
        Commands commands,
        Scheduler async,
        RegionScheduler scheduler,
        EntityScheduler entityScheduler,
        YaminabeReloader reloader,
        RestartService restartService,
        Supplier<RestartCommandSettings> restartSettings,
        boolean folia
    ) {
        commands.register(
            Commands.literal("yaminabe")
                .requires(source -> source.getSender().hasPermission("yaminabe.command"))
                .then(DumpCommandsCommand.createDumpCommandsCommand(commands.getDispatcher()))
                .then(ReloadCommand.createReloadCommand(async, reloader))
                .then(VersionCommand.createVersionCommand())
                .build()
        );

        commands.register(
            createAutoRestartCommand(restartService, Clock.systemUTC(), restartSettings),
            List.of("are")
        );
        // Folia disables the server's native /restart command. Plain Paper keeps its built-in command,
        // so Yaminabe only claims this top-level label when running on Folia.
        if (folia) {
            commands.register(createRestartCommand(restartService, Clock.systemUTC(), restartSettings));
        }

        commands.register(DisposalCommand.createDisposalCommand(), DisposalCommand.getAliases());
        commands.register(HatCommand.createHatCommand(), HatCommand.getAliases());
        commands.register(ItemCommand.createItemCommand(), ItemCommand.getAliases());
        commands.register(ItemLoreCommand.createItemLoreCommand(), ItemLoreCommand.getAliases());
        commands.register(ItemNameCommand.createItemNameCommand(), ItemNameCommand.getAliases());
        commands.register(PTimeCommand.createPTimeCommand(entityScheduler));
        commands.register(PWeatherCommand.createPWeatherCommand(entityScheduler));
        commands.register(SignCommand.createSignCommand(scheduler), SignCommand.getAliases());
        commands.register(SkullCommand.createSkullCommand());

        for (WorkstationCommands workstation : WorkstationCommands.values()) {
            commands.register(workstation.createCommand(), workstation.getAliases());
        }
    }

    static com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> createAutoRestartCommand(
        RestartService service,
        Clock clock,
        Supplier<RestartCommandSettings> settingsSupplier
    ) {
        return AutoRestartCommand.create(
            "autorestart",
            service,
            clock,
            settingsSupplier,
            RESTART_SOURCE
        );
    }

    static com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> createRestartCommand(
        RestartService service,
        Clock clock,
        Supplier<RestartCommandSettings> settingsSupplier
    ) {
        return RestartNowCommand.create(
            "restart",
            service,
            clock,
            settingsSupplier,
            RESTART_SOURCE
        );
    }

    private YaminabeCommands() {
        throw new UnsupportedOperationException();
    }
}
