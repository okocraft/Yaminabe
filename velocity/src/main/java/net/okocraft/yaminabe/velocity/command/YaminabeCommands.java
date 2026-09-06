package net.okocraft.yaminabe.velocity.command;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
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
import org.jetbrains.annotations.NotNullByDefault;

import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;

@NotNullByDefault
public final class YaminabeCommands {

    private static final RestartCommandSource<CommandSource> RESTART_SOURCE = new RestartCommandSource<>() {
        @Override
        public boolean hasPermission(CommandSource source, String permission) {
            return source.hasPermission(permission);
        }

        @Override
        public void sendMessage(CommandSource source, ComponentLike message) {
            source.sendMessage(message.asComponent());
        }
    };

    public static DefaultMessageDefiner getDefiner() {
        return CommandMessages.DEFINER;
    }

    public static List<DefaultMessageDefiner> getDefiners() {
        return List.of(
            CommandMessages.DEFINER,
            RestartCommandMessages.DEFINER,
            RestartExecutionMessages.DEFINER,
            RestartCountdownMessages.DEFINER
        );
    }

    public static void register(
        CommandManager manager,
        Object plugin,
        Scheduler scheduler,
        YaminabeReloader reloader,
        RestartService restartService,
        Supplier<RestartCommandSettings> restartSettings
    ) {
        BrigadierCommand yaminabe = createCommand(scheduler, reloader);
        manager.register(manager.metaBuilder(yaminabe).plugin(plugin).build(), yaminabe);

        BrigadierCommand autoRestart = createAutoRestartCommand(restartService, Clock.systemUTC(), restartSettings);
        manager.register(
            manager.metaBuilder(autoRestart).aliases("are", "vare").plugin(plugin).build(),
            autoRestart
        );

        BrigadierCommand velocityRestart = createVelocityRestartCommand(restartService, Clock.systemUTC(), restartSettings);
        manager.register(manager.metaBuilder(velocityRestart).plugin(plugin).build(), velocityRestart);
    }

    static BrigadierCommand createCommand(Scheduler scheduler, YaminabeReloader reloader) {
        return new BrigadierCommand(
            BrigadierCommand.literalArgumentBuilder("yaminabe")
                .requires(source -> source.hasPermission("yaminabe.command"))
                .then(ReloadCommand.createReloadCommand(scheduler, reloader))
                .then(VersionCommand.createVersionCommand())
        );
    }

    static BrigadierCommand createAutoRestartCommand(
        RestartService service,
        Clock clock,
        Supplier<RestartCommandSettings> settingsSupplier
    ) {
        return new BrigadierCommand(AutoRestartCommand.create(
            "autorestart",
            service,
            clock,
            settingsSupplier,
            RESTART_SOURCE
        ));
    }

    static BrigadierCommand createVelocityRestartCommand(
        RestartService service,
        Clock clock,
        Supplier<RestartCommandSettings> settingsSupplier
    ) {
        return new BrigadierCommand(RestartNowCommand.create(
            "vrestart",
            service,
            clock,
            settingsSupplier,
            RESTART_SOURCE
        ));
    }

    private YaminabeCommands() {
        throw new UnsupportedOperationException();
    }
}
