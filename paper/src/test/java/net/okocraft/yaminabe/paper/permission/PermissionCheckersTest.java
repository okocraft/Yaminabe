package net.okocraft.yaminabe.paper.permission;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PermissionCheckersTest {

    @Test
    void testUnsetNodeIsReportedAsNotSet() {
        Permissible permissible = Mockito.mock(Permissible.class);
        PermissionChecker checker = PermissionCheckers.of(permissible);

        Mockito.when(permissible.isPermissionSet("granted")).thenReturn(true);
        Mockito.when(permissible.hasPermission("granted")).thenReturn(true);
        Mockito.when(permissible.isPermissionSet("denied")).thenReturn(true);
        Mockito.when(permissible.hasPermission("denied")).thenReturn(false);

        Assertions.assertEquals(TriState.TRUE, checker.value("granted"));
        Assertions.assertEquals(TriState.FALSE, checker.value("denied"));
        Assertions.assertEquals(TriState.NOT_SET, checker.value("unset"));
    }
}
