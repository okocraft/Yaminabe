package net.okocraft.yaminabe.paper.command;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import io.papermc.paper.command.brigadier.Commands;
import net.okocraft.yaminabe.common.YaminabeReloader;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.paper.platform.RegionScheduler;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class YaminabeCommands {

    public static DefaultMessageDefiner getDefiner() {
        return CommandMessages.DEFINER;
    }

    public static void register(Commands commands, Scheduler async, RegionScheduler scheduler, YaminabeReloader reloader) {
        commands.register(
            Commands.literal("yaminabe")
                .requires(source -> source.getSender().hasPermission("yaminabe.command"))
                .then(DumpCommandsCommand.createDumpCommandsCommand(commands.getDispatcher()))
                .then(ReloadCommand.createReloadCommand(async, reloader))
                .then(VersionCommand.createVersionCommand())
                .build()
        );

        commands.register(DisposalCommand.createDisposalCommand(), DisposalCommand.getAliases());
        commands.register(HatCommand.createHatCommand(), HatCommand.getAliases());
        commands.register(ItemCommand.createItemCommand(), ItemCommand.getAliases());
        commands.register(ItemLoreCommand.createItemLoreCommand(), ItemLoreCommand.getAliases());
        commands.register(ItemNameCommand.createItemNameCommand(), ItemNameCommand.getAliases());
        commands.register(SignCommand.createSignCommand(scheduler), SignCommand.getAliases());
        commands.register(SkullCommand.createSkullCommand());

        for (WorkstationCommands workstation : WorkstationCommands.values()) {
            commands.register(workstation.createCommand(), workstation.getAliases());
        }
    }

}
