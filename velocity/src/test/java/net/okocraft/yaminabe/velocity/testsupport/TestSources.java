package net.okocraft.yaminabe.velocity.testsupport;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import org.mockito.Mockito;

/**
 * Creates command sources with Velocity's tristate permission behavior preserved.
 */
public final class TestSources {

    public static ConsoleCommandSource console() {
        return create(ConsoleCommandSource.class);
    }

    public static Player player() {
        return create(Player.class);
    }

    public static void grant(CommandSource source, String... permissions) {
        for (String permission : permissions) {
            Mockito.when(source.getPermissionValue(permission)).thenReturn(Tristate.TRUE);
        }
    }

    public static void deny(CommandSource source, String... permissions) {
        for (String permission : permissions) {
            Mockito.when(source.getPermissionValue(permission)).thenReturn(Tristate.FALSE);
        }
    }

    private static <T extends CommandSource> T create(Class<T> type) {
        T source = Mockito.mock(type);
        Mockito.when(source.getPermissionValue(Mockito.anyString())).thenReturn(Tristate.UNDEFINED);
        Mockito.when(source.hasPermission(Mockito.anyString())).thenCallRealMethod();
        return source;
    }

    private TestSources() {
        throw new UnsupportedOperationException();
    }
}
