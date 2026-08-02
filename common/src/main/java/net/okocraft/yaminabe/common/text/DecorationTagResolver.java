package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A {@link TagResolver} that delegates to {@link StandardTags#decorations(TextDecoration)} only for the decorations the
 * player is allowed to use.
 * <p>
 * Each decoration is checked when the tag is resolved, as {@link ColorTagResolver} does for colors, so that the two
 * halves of a permission change never take effect at different times. Decorations that are not allowed are left as
 * they were typed.
 */
@NotNullByDefault
final class DecorationTagResolver implements TagResolver {

    private static final Map<TextDecoration, TagResolver> DELEGATES = createDelegates();
    private static final TagResolver ALL = StandardTags.decorations();

    private static Map<TextDecoration, TagResolver> createDelegates() {
        Map<TextDecoration, TagResolver> delegates = new EnumMap<>(TextDecoration.class);

        for (TextDecoration decoration : TextDecoration.values()) {
            delegates.put(decoration, StandardTags.decorations(decoration));
        }

        return delegates;
    }

    static TagResolver create(PermissionChecker checker, String permissionNode) {
        return new DecorationTagResolver(checker, permissionNode);
    }

    private final PermissionChecker checker;
    private final String groupNode;
    private final String decorationNodePrefix;

    private DecorationTagResolver(PermissionChecker checker, String permissionNode) {
        this.checker = checker;
        this.groupNode = permissionNode;
        this.decorationNodePrefix = permissionNode + ".";
    }

    @Override
    public @Nullable Tag resolve(String name, ArgumentQueue arguments, Context ctx) throws ParsingException {
        // A decoration is written in several ways (<bold>, <b>, <!bold>, ...), so the delegate decides which one it is.
        for (Map.Entry<TextDecoration, TagResolver> entry : DELEGATES.entrySet()) {
            if (entry.getValue().has(name)) {
                return isAllowed(entry.getKey()) ? entry.getValue().resolve(name, arguments, ctx) : null;
            }
        }

        return null;
    }

    @Override
    public boolean has(String name) {
        return ALL.has(name);
    }

    private boolean isAllowed(TextDecoration decoration) {
        String node = decorationNodePrefix + TextDecoration.NAMES.keyOrThrow(decoration);
        return checker.value(node).toBooleanOrElseGet(() -> checker.test(groupNode));
    }
}
