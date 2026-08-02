package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PermissionChecker} factory for tests.
 */
final class TestPermissions {

    /**
     * Creates a {@link PermissionChecker} that reports the given nodes as granted, the nodes prefixed with {@code -} as
     * denied, and everything else as unset.
     *
     * @param nodes the permission nodes to set
     * @return the created {@link PermissionChecker}
     */
    static PermissionChecker set(String... nodes) {
        Map<String, TriState> states = new HashMap<>();

        Arrays.stream(nodes).forEach(node ->
            states.put(node.startsWith("-") ? node.substring(1) : node, TriState.byBoolean(!node.startsWith("-")))
        );

        return backedBy(states);
    }

    /**
     * Creates a {@link PermissionChecker} that reports every node as granted.
     *
     * @return the created {@link PermissionChecker}
     */
    static PermissionChecker all() {
        return node -> TriState.TRUE;
    }

    /**
     * Creates a {@link PermissionChecker} backed by the given states, which is mutable so that a test can change a
     * permission after the resolver has been created.
     *
     * @param states the states of the permission nodes
     * @return the created {@link PermissionChecker}
     */
    static PermissionChecker backedBy(Map<String, TriState> states) {
        return node -> states.getOrDefault(node, TriState.NOT_SET);
    }

    private TestPermissions() {
        throw new UnsupportedOperationException();
    }
}
