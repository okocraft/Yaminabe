package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.Set;

class MiniMessageTextTest {

    private static final String BASE = "test.format";
    private static final Set<FormatTag> TAGS = EnumSet.of(FormatTag.COLOR, FormatTag.DECORATION);

    private static final int MAX_LENGTH = 200;

    /* parse */

    @Test
    void testParseResolvesOnlyTheAllowedTags() {
        Assertions.assertEquals(
            Component.text("text", NamedTextColor.RED),
            MiniMessageText.parse(TestPermissions.set(BASE + ".color"), BASE, TAGS, "<red>text")
        );

        Assertions.assertEquals(
            Component.text("<red>text"),
            MiniMessageText.parse(TestPermissions.set(), BASE, TAGS, "<red>text")
        );
    }

    /* withItalicOff */

    @Test
    void testItalicIsTurnedOffOnlyWhenItIsNotSet() {
        Assertions.assertEquals(
            Component.text("text").decoration(TextDecoration.ITALIC, false),
            MiniMessageText.withItalicOff(Component.text("text"))
        );

        Component italic = Component.text("text").decoration(TextDecoration.ITALIC, true);
        Assertions.assertEquals(italic, MiniMessageText.withItalicOff(italic));
    }

    /* toEditableSource */

    @ParameterizedTest
    @ValueSource(strings = {"text", "<red>text", "<bold>text", "<red><bold>text", "<red>text</red> and text"})
    void testTextThatCanBeRetypedIsSerialized(String source) {
        PermissionChecker checker = TestPermissions.all();
        Component text = MiniMessageText.parse(checker, BASE, TAGS, source);
        String serialized = MiniMessageText.toEditableSource(checker, BASE, TAGS, text, MAX_LENGTH);

        Assertions.assertNotNull(serialized, source);

        // The serializer writes the tags in its own order, so what matters is that retyping reproduces the text.
        Assertions.assertEquals(text.compact(), MiniMessageText.parse(checker, BASE, TAGS, serialized).compact(), source);
    }

    @Test
    void testItalicTurnedOffByWithItalicOffIsLeftOut() {
        PermissionChecker checker = TestPermissions.all();
        Component text = MiniMessageText.withItalicOff(MiniMessageText.parse(checker, BASE, TAGS, "<red>text"));

        Assertions.assertEquals("<red>text", MiniMessageText.toEditableSource(checker, BASE, TAGS, text, MAX_LENGTH));
    }

    @Test
    void testItalicSetExplicitlyIsKept() {
        PermissionChecker checker = TestPermissions.all();
        Component text = MiniMessageText.withItalicOff(MiniMessageText.parse(checker, BASE, TAGS, "<italic>text"));

        Assertions.assertEquals("<italic>text", MiniMessageText.toEditableSource(checker, BASE, TAGS, text, MAX_LENGTH));
    }

    @Test
    void testTextUsingATagThatIsNotAllowedIsNotSerialized() {
        Component text = Component.text("text", NamedTextColor.RED);

        // The color would be left as the six characters "<red>" if it were retyped.
        Assertions.assertNull(MiniMessageText.toEditableSource(TestPermissions.set(), BASE, TAGS, text, MAX_LENGTH));
        Assertions.assertNull(MiniMessageText.toEditableSource(TestPermissions.all(), BASE, EnumSet.of(FormatTag.DECORATION), text, MAX_LENGTH));
    }

    @Test
    void testTextUsingATagThatIsNotOfferedAtAllIsNotSerialized() {
        Component text = Component.text("text").hoverEvent(Component.text("hover"));

        Assertions.assertNull(MiniMessageText.toEditableSource(TestPermissions.all(), BASE, TAGS, text, MAX_LENGTH));
    }

    @Test
    void testTooLongTextIsNotSerialized() {
        Component text = Component.text("text");

        Assertions.assertEquals("text", MiniMessageText.toEditableSource(TestPermissions.all(), BASE, TAGS, text, 4));
        Assertions.assertNull(MiniMessageText.toEditableSource(TestPermissions.all(), BASE, TAGS, text, 3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"§ctext", "line\nline", "text\u007f"})
    void testTextThatCannotBeTypedInACommandIsNotSerialized(String content) {
        Component text = Component.text(content);

        Assertions.assertNull(MiniMessageText.toEditableSource(TestPermissions.all(), BASE, TAGS, text, MAX_LENGTH));
    }
}
