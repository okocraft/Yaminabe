package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * A group of MiniMessage tags that can be enabled by a permission.
 * <p>
 * Which groups are offered to the player at all is decided by the caller of
 * {@link FormatPermissions#createSerializer}, and each offered group is then enabled only if the player has
 * {@code <base>.<permission name>}.
 */
@NotNullByDefault
public enum FormatTag {

    /**
     * Color tags such as {@code <red>}, {@code <color:red>} and {@code <#ff0000>}.
     * <p>
     * A color can be allowed or forbidden individually by explicitly setting {@code <base>.color.<name>}.
     * Hexadecimal colors are covered by {@code <base>.color.hex}.
     * <p>
     * These nodes only constrain this group. {@link #GRADIENT}, {@link #RAINBOW}, {@link #TRANSITION} and
     * {@link #SHADOW_COLOR} resolve their colors inside MiniMessage and are therefore constrained by their own group
     * node alone.
     */
    COLOR("color", ColorTagResolver::create),

    /**
     * Decoration tags such as {@code <bold>} and {@code <obfuscated>}.
     * <p>
     * A decoration can be allowed or forbidden individually by explicitly setting {@code <base>.decoration.<name>}.
     */
    DECORATION("decoration", DecorationTagResolver::create),

    /**
     * The {@code <gradient>} tag.
     */
    GRADIENT("gradient", grouped(StandardTags.gradient())),

    /**
     * The {@code <rainbow>} tag.
     */
    RAINBOW("rainbow", grouped(StandardTags.rainbow())),

    /**
     * The {@code <transition>} tag.
     */
    TRANSITION("transition", grouped(StandardTags.transition())),

    /**
     * The {@code <shadow>} tag. Note that the permission name ({@code shadow-color}) does not match the tag name.
     */
    SHADOW_COLOR("shadow-color", grouped(StandardTags.shadowColor())),

    /**
     * The {@code <font>} tag.
     */
    FONT("font", grouped(StandardTags.font())),

    /**
     * The {@code <click>} tag.
     */
    CLICK("click", grouped(StandardTags.clickEvent())),

    /**
     * The {@code <hover>} tag.
     */
    HOVER("hover", grouped(StandardTags.hoverEvent())),

    /**
     * The {@code <insert>} tag.
     */
    INSERTION("insertion", grouped(StandardTags.insertion())),

    /**
     * The {@code <lang>}, {@code <lang_or>} and {@code <key>} tags, and their aliases such as {@code <translate>} and {@code <tr>}.
     */
    TRANSLATABLE("translatable", grouped(TagResolver.resolver(StandardTags.translatable(), StandardTags.translatableFallback(), StandardTags.keybind()))),

    /**
     * The {@code <reset>} tag.
     */
    RESET("reset", grouped(StandardTags.reset())),

    /**
     * The {@code <newline>} tag and its alias {@code <br>}.
     */
    NEWLINE("newline", grouped(StandardTags.newline()));

    private final String permissionName;
    private final ResolverFactory factory;

    FormatTag(String permissionName, ResolverFactory factory) {
        this.permissionName = permissionName;
        this.factory = factory;
    }

    /**
     * Gets the name of the permission node this group is placed under.
     *
     * @return the name of the permission node this group is placed under
     */
    public String permissionName() {
        return permissionName;
    }

    TagResolver createResolver(PermissionChecker checker, String permissionPrefix) {
        return factory.create(checker, permissionPrefix + permissionName);
    }

    private static ResolverFactory grouped(TagResolver resolver) {
        return (checker, node) -> GroupedTagResolver.create(resolver, checker, node);
    }

    @FunctionalInterface
    interface ResolverFactory {
        TagResolver create(PermissionChecker checker, String permissionNode);
    }
}
