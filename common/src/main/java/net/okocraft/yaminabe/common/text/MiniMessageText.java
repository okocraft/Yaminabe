package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Text that a player writes in MiniMessage format and that is then shown on an item, such as an item name or a lore
 * line.
 * <p>
 * Which tags the player may use is decided by {@link FormatPermissions}, so text written this way can only be
 * reproduced as it is by the player allowed to use every tag it holds. {@link #toEditableSource} is about that
 * question, and is what lets such text be suggested for editing instead of being retyped.
 */
@NotNullByDefault
public final class MiniMessageText {

    private static final char SECTION_SIGN = '§';
    private static final char DELETE = '\u007f';

    /**
     * Parses the given text, resolving only the tags the given {@link PermissionChecker} allows.
     *
     * @param checker the {@link PermissionChecker} to check permissions against
     * @param permissionBase the permission node the tag groups are placed under
     * @param tags the {@link FormatTag}s to offer
     * @param text the text to parse
     * @return the parsed {@link Component}
     */
    public static Component parse(PermissionChecker checker, String permissionBase, Set<FormatTag> tags, String text) {
        return FormatPermissions.createSerializer(checker, permissionBase, tags).deserialize(text);
    }

    /**
     * Turns off the italic decoration of the given text unless it is set explicitly, as an item name and lore lines
     * are italicized by default.
     *
     * @param text the text to turn the italic decoration off of
     * @return the given text with the italic decoration turned off
     */
    public static Component withItalicOff(Component text) {
        return text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Serializes the given text back to MiniMessage so that it can be suggested for editing, or returns {@code null}
     * if accepting the suggestion would not reproduce the text as it is.
     * <p>
     * That happens when the text is longer than {@code maxLength} characters, holds a character that cannot be typed
     * in a command, or uses a tag the given {@link PermissionChecker} does not allow or one that is not offered by
     * {@code tags}, which text set by an operator or by another plugin can carry.
     * <p>
     * The {@code <!italic>} of {@link #withItalicOff} is left out, so that what is suggested is what was typed.
     *
     * @param checker the {@link PermissionChecker} to check permissions against
     * @param permissionBase the permission node the tag groups are placed under
     * @param tags the {@link FormatTag}s to offer
     * @param text the text to serialize
     * @param maxLength the maximum length of the serialized text
     * @return the serialized text, or {@code null} if it cannot be retyped as it is
     */
    public static @Nullable String toEditableSource(PermissionChecker checker, String permissionBase, Set<FormatTag> tags, Component text, int maxLength) {
        Component typed = withoutItalicOff(text);
        String serialized = MiniMessage.miniMessage().serialize(typed);

        if (maxLength < serialized.length() || !isTypableInCommand(serialized)) {
            return null;
        }

        return withoutItalicOff(parse(checker, permissionBase, tags, serialized)).compact().equals(typed.compact()) ? serialized : null;
    }

    private static Component withoutItalicOff(Component text) {
        return text.decoration(TextDecoration.ITALIC) == TextDecoration.State.FALSE ?
            text.decoration(TextDecoration.ITALIC, TextDecoration.State.NOT_SET) :
            text;
    }

    private static boolean isTypableInCommand(String text) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (character == SECTION_SIGN || character < ' ' || character == DELETE) {
                return false;
            }
        }

        return true;
    }

    private MiniMessageText() {
        throw new UnsupportedOperationException();
    }
}
