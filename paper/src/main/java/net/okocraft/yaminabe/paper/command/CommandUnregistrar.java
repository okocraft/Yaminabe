package net.okocraft.yaminabe.paper.command;

import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Collection;
import java.util.Locale;

@NotNullByDefault
final class CommandUnregistrar {

    static void unregister(Commands commands, Collection<String> labels) {
        unregister(commands, Bukkit.getCommandMap(), labels);
    }

    static void unregister(Commands commands, CommandMap commandMap, Collection<String> labels) {
        var knownCommands = commandMap.getKnownCommands();
        var root = commands.getDispatcher().getRoot();

        for (String rawLabel : labels) {
            String label = normalize(rawLabel);
            if (label.isEmpty()) {
                continue;
            }

            knownCommands.remove(label);
            root.removeCommand(label);
        }
    }

    private static String normalize(String label) {
        String normalized = label.strip();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ENGLISH);
    }

    private CommandUnregistrar() {
    }
}
