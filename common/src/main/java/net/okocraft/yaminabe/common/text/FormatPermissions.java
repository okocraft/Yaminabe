package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A factory of {@link MiniMessage} serializers that only resolve the tags a player is allowed to use.
 * <p>
 * The caller decides which {@link FormatTag}s make sense where the text is used; tags outside that set are never
 * resolved, no matter what permissions the player has. Each offered group is then enabled by
 * {@code <base>.<permission name>}, and colors and decorations can be allowed or forbidden individually.
 * <p>
 * Where an individual color or decoration is checked, {@link net.kyori.adventure.util.TriState#NOT_SET} means "fall
 * back to the group node", not "denied", so the given {@link PermissionChecker} is expected to tell an unset node from
 * a denied one.
 * <p>
 * Tags that are not resolved are left as they were typed by {@link MiniMessage#deserialize}, which is what these
 * serializers are for. The other directions are not: {@link MiniMessage#escapeTags} and {@link MiniMessage#stripTags}
 * only consult {@link TagResolver#has}, so they do not make the same distinction, and {@link MiniMessage#serialize}
 * drops every style, as these resolvers do not claim any.
 */
@NotNullByDefault
public final class FormatPermissions {

    /**
     * Creates a {@link MiniMessage} that resolves only the tags the given {@link PermissionChecker} allows.
     *
     * @param checker the {@link PermissionChecker} to check permissions against
     * @param permissionBase the permission node the tag groups are placed under
     * @param tags the {@link FormatTag}s to offer
     * @return the created {@link MiniMessage}
     */
    public static MiniMessage createSerializer(PermissionChecker checker, String permissionBase, Set<FormatTag> tags) {
        return MiniMessage.builder().tags(createTagResolver(checker, permissionBase, tags)).build();
    }

    /**
     * Creates a {@link TagResolver} that resolves only the tags the given {@link PermissionChecker} allows.
     *
     * @param checker the {@link PermissionChecker} to check permissions against
     * @param permissionBase the permission node the tag groups are placed under
     * @param tags the {@link FormatTag}s to offer
     * @return the created {@link TagResolver}
     */
    public static TagResolver createTagResolver(PermissionChecker checker, String permissionBase, Set<FormatTag> tags) {
        String prefix = permissionBase + ".";
        List<TagResolver> resolvers = new ArrayList<>(tags.size());

        for (FormatTag tag : tags) {
            resolvers.add(tag.createResolver(checker, prefix));
        }

        return TagResolver.resolver(resolvers);
    }

    private FormatPermissions() {
        throw new UnsupportedOperationException();
    }
}
