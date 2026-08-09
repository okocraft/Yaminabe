package net.okocraft.yaminabe.paper.permission;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.okocraft.yaminabe.common.text.FormatPermissions;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * A {@link PermissionChecker} factory for Bukkit.
 */
@NotNullByDefault
public final class PermissionCheckers {

    /**
     * Creates a {@link PermissionChecker} that reports whether a permission is unset, granted or denied for the given
     * {@link Permissible}.
     * <p>
     * {@link TriState#NOT_SET} is only reported for a node that is not set at all, which
     * {@link FormatPermissions} reads as "fall back to the group node".
     *
     * @param permissible the {@link Permissible} to check permissions of
     * @return the created {@link PermissionChecker}
     */
    public static PermissionChecker of(Permissible permissible) {
        return node -> permissible.isPermissionSet(node) ? TriState.byBoolean(permissible.hasPermission(node)) : TriState.NOT_SET;
    }

    private PermissionCheckers() {
        throw new UnsupportedOperationException();
    }
}
