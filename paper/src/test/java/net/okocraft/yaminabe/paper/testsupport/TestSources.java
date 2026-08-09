package net.okocraft.yaminabe.paper.testsupport;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.mockito.Mockito;

/**
 * The {@link CommandSourceStack} a command is run with.
 */
public final class TestSources {

    /**
     * Creates a source that the given player runs a command with, as both its sender and its executor.
     */
    public static CommandSourceStack of(Player player) {
        return of(player, player);
    }

    /**
     * Creates a source that the given sender runs a command with, with no executor, the way the console does.
     */
    public static CommandSourceStack ofSenderOnly(CommandSender sender) {
        return of(sender, null);
    }

    public static CommandSourceStack of(CommandSender sender, Player executor) {
        CommandSourceStack source = Mockito.mock(CommandSourceStack.class);
        Mockito.when(source.getSender()).thenReturn(sender);
        Mockito.when(source.getExecutor()).thenReturn(executor);
        return source;
    }

    /**
     * Grants the given permissions, leaving the ones not given here unset rather than denied, which is the difference
     * {@link net.okocraft.yaminabe.paper.permission.PermissionCheckers} is about.
     */
    public static void grant(Permissible permissible, String... permissions) {
        for (String permission : permissions) {
            Mockito.when(permissible.isPermissionSet(permission)).thenReturn(true);
            Mockito.when(permissible.hasPermission(permission)).thenReturn(true);
        }
    }

    /**
     * Denies the given permissions, setting them to {@code false} rather than leaving them unset, which is what a
     * permission plugin does for a node written with a {@code false} value.
     */
    public static void deny(Permissible permissible, String... permissions) {
        for (String permission : permissions) {
            Mockito.when(permissible.isPermissionSet(permission)).thenReturn(true);
            Mockito.when(permissible.hasPermission(permission)).thenReturn(false);
        }
    }

    private TestSources() {
        throw new UnsupportedOperationException();
    }
}
