package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.util.TriState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertLeftAsTyped;
import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertResolved;
import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertResolvedTo;

class DecorationTagResolverTest {

    private static final String GROUP_NODE = "test.decoration";
    private static final String BOLD_NODE = GROUP_NODE + ".bold";

    private static final Component BOLD_TEXT = Component.text("text", null, TextDecoration.BOLD);

    /* The group node */

    @Test
    void testDecorationIsResolvedByGroupNode() {
        MiniMessage serializer = serializer(TestPermissions.set(GROUP_NODE));

        assertResolvedTo(serializer, "<bold>text", BOLD_TEXT);
        assertResolvedTo(serializer, "<obfuscated>text", Component.text("text", null, TextDecoration.OBFUSCATED));
    }

    @Test
    void testDecorationIsLeftAsTypedWithoutPermission() {
        assertLeftAsTyped(serializer(TestPermissions.set()), "<bold>text");
    }

    /* The node of an individual decoration */

    @Test
    void testDecorationIsResolvedByItsOwnNode() {
        MiniMessage serializer = serializer(TestPermissions.set(BOLD_NODE));

        assertResolvedTo(serializer, "<bold>text", BOLD_TEXT);
        assertLeftAsTyped(serializer, "<italic>text");
    }

    @Test
    void testDeniedDecorationNodeOverridesGroupNode() {
        MiniMessage serializer = serializer(TestPermissions.set(GROUP_NODE, "-" + BOLD_NODE));

        assertLeftAsTyped(serializer, "<bold>text");
        assertResolvedTo(serializer, "<italic>text", Component.text("text", null, TextDecoration.ITALIC));
    }

    /**
     * The resolver picks the delegate by tag name, so every way of writing a decoration must reach the node of its
     * canonical name, and none of them may fall through to another decoration.
     */
    @ParameterizedTest
    @CsvSource({
        "<bold>text, bold", "<b>text, bold", "<bold:true>text, bold", "<!bold>text, bold", "<!b>text, bold",
        "<italic>text, italic", "<i>text, italic", "<em>text, italic",
        "<underlined>text, underlined", "<u>text, underlined",
        "<strikethrough>text, strikethrough", "<st>text, strikethrough",
        "<obfuscated>text, obfuscated", "<obf>text, obfuscated"
    })
    void testEveryWayOfWritingADecorationIsCheckedAgainstTheSameNode(String input, String canonicalName) {
        String canonicalNode = GROUP_NODE + "." + canonicalName;

        assertResolved(serializer(TestPermissions.set(canonicalNode)), input);
        assertLeftAsTyped(serializer(TestPermissions.set(GROUP_NODE, "-" + canonicalNode)), input);
    }

    /* When the permission is read */

    @Test
    void testPermissionIsCheckedOnEveryUse() {
        Map<String, TriState> states = new HashMap<>();
        MiniMessage serializer = serializer(TestPermissions.backedBy(states));

        assertLeftAsTyped(serializer, "<bold>text");

        states.put(GROUP_NODE, TriState.TRUE);

        assertResolvedTo(serializer, "<bold>text", BOLD_TEXT);
    }

    private static MiniMessage serializer(PermissionChecker checker) {
        return MiniMessage.builder().tags(DecorationTagResolver.create(checker, GROUP_NODE)).build();
    }
}
