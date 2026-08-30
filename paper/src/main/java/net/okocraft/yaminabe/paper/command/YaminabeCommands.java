package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.paper.platform.RegionScheduler;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NotNullByDefault
public final class YaminabeCommands {

    public static DefaultMessageDefiner getDefiner() {
        return CommandMessages.DEFINER;
    }

    public static void register(
        Commands commands,
        Scheduler async,
        RegionScheduler scheduler,
        YaminabeReloader reloader,
        Collection<String> additionalCommandsToUnregister
    ) {
        CommandUnregistrar.unregister(commands, additionalCommandsToUnregister);

        register(
            commands,
            Commands.literal("yaminabe")
                .requires(source -> source.getSender().hasPermission("yaminabe.command"))
                .then(DumpCommandsCommand.createDumpCommandsCommand(commands.getDispatcher()))
                .then(ReloadCommand.createReloadCommand(async, reloader))
                .then(VersionCommand.createVersionCommand())
                .build(),
            List.of()
        );

        register(commands, DisposalCommand.createDisposalCommand(), DisposalCommand.getAliases());
        register(commands, HatCommand.createHatCommand(), HatCommand.getAliases());
        register(commands, ItemCommand.createItemCommand(), ItemCommand.getAliases());
        register(commands, ItemLoreCommand.createItemLoreCommand(), ItemLoreCommand.getAliases());
        register(commands, ItemNameCommand.createItemNameCommand(), ItemNameCommand.getAliases());
        register(commands, SignCommand.createSignCommand(scheduler), SignCommand.getAliases());
        register(commands, SkullCommand.createSkullCommand(), List.of());

        for (WorkstationCommands workstation : WorkstationCommands.values()) {
            register(commands, workstation.createCommand(), workstation.getAliases());
        }
    }

    private static void register(
        Commands commands,
        LiteralCommandNode<CommandSourceStack> command,
        Collection<String> aliases
    ) {
        var labels = new ArrayList<String>(aliases.size() + 1);
        labels.add(command.getLiteral());
        labels.addAll(aliases);

        CommandUnregistrar.unregister(commands, labels);
        commands.register(command, aliases);
    }

}
