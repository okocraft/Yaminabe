package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * A {@link TagResolver} that delegates to {@link StandardTags#color()} only for the colors the player is allowed to
 * use.
 * <p>
 * Unlike decorations, MiniMessage does not offer a resolver per color, so the color is taken from the tag itself and
 * checked before the tag is resolved. Colors that are not allowed are left as they were typed.
 */
@NotNullByDefault
final class ColorTagResolver implements TagResolver {

    /**
     * The permission node name for colors that are not one of {@link NamedTextColor}, that is, hexadecimal colors.
     */
    static final String HEX_COLOR_NAME = "hex";

    private static final TagResolver DELEGATE = StandardTags.color();

    /**
     * The tags that take the color as their first argument, instead of being the color itself.
     * <p>
     * {@code colour} is not our spelling, but the alias MiniMessage itself defines, so it must be recognized here.
     */
    private static final Set<String> COLOR_ARGUMENT_TAGS = Set.of("color", "colour", "c");

    /**
     * The color names MiniMessage accepts but {@link NamedTextColor#NAMES} does not know about.
     * <p>
     * Without this, {@code <grey>} would be checked against the node of a hexadecimal color instead of the node of
     * {@code gray}, which would let a player bypass a denied color by spelling it differently.
     */
    private static final Map<String, String> COLOR_ALIASES = Map.of("grey", "gray", "dark_grey", "dark_gray");

    static TagResolver create(PermissionChecker checker, String permissionNode) {
        return new ColorTagResolver(checker, permissionNode);
    }

    private final PermissionChecker checker;
    private final String groupNode;
    private final String colorNodePrefix;

    private ColorTagResolver(PermissionChecker checker, String permissionNode) {
        this.checker = checker;
        this.groupNode = permissionNode;
        this.colorNodePrefix = permissionNode + ".";
    }

    @Override
    public @Nullable Tag resolve(String name, ArgumentQueue arguments, Context ctx) throws ParsingException {
        if (!DELEGATE.has(name)) {
            return null;
        }

        String color = extractColor(name, arguments);

        return color != null && isAllowed(color) ? DELEGATE.resolve(name, arguments, ctx) : null;
    }

    @Override
    public boolean has(String name) {
        return DELEGATE.has(name);
    }

    /**
     * Gets the color the given tag refers to, or {@code null} if the tag carries no color.
     */
    private static @Nullable String extractColor(String name, ArgumentQueue arguments) {
        if (!COLOR_ARGUMENT_TAGS.contains(name)) {
            return name;
        }

        // peek does not consume the argument, so the delegate can still read it.
        Tag.Argument argument = arguments.hasNext() ? arguments.peek() : null;
        return argument != null ? argument.lowerValue() : null;
    }

    private boolean isAllowed(String color) {
        String name = COLOR_ALIASES.getOrDefault(color, color);
        String node = colorNodePrefix + (NamedTextColor.NAMES.value(name) != null ? name : HEX_COLOR_NAME);
        return checker.value(node).toBooleanOrElseGet(() -> checker.test(groupNode));
    }
}
