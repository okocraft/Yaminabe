package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link TagResolver} that delegates to another one only while a single permission node is granted.
 * <p>
 * This is used for the groups that are enabled as a whole. The permission is checked when the tag is resolved, as
 * {@link ColorTagResolver} and {@link DecorationTagResolver} do, so that a permission change takes effect at the same
 * time for every group. Tags that are not allowed are left as they were typed.
 */
@NotNullByDefault
final class GroupedTagResolver implements TagResolver {

    static TagResolver create(TagResolver delegate, PermissionChecker checker, String permissionNode) {
        return new GroupedTagResolver(delegate, checker, permissionNode);
    }

    private final TagResolver delegate;
    private final PermissionChecker checker;
    private final String permissionNode;

    private GroupedTagResolver(TagResolver delegate, PermissionChecker checker, String permissionNode) {
        this.delegate = delegate;
        this.checker = checker;
        this.permissionNode = permissionNode;
    }

    @Override
    public @Nullable Tag resolve(String name, ArgumentQueue arguments, Context ctx) throws ParsingException {
        return delegate.has(name) && checker.test(permissionNode) ? delegate.resolve(name, arguments, ctx) : null;
    }

    @Override
    public boolean has(String name) {
        return delegate.has(name);
    }
}
