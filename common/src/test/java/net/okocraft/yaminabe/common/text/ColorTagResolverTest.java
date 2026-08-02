package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.util.TriState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertLeftAsTyped;
import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertResolved;
import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertResolvedTo;

class ColorTagResolverTest {

    private static final String GROUP_NODE = "test.color";
    private static final String RED_NODE = GROUP_NODE + ".red";
    private static final String HEX_NODE = GROUP_NODE + "." + ColorTagResolver.HEX_COLOR_NAME;

    /**
     * The ways of writing the same color, all of which must be checked against the same node.
     */
    private static final String RED_INPUTS = "<red>text, <color:red>text, <colour:red>text, <c:red>text";

    private static final Component RED_TEXT = Component.text("text", NamedTextColor.RED);

    /* The group node */

    @ParameterizedTest
    @ValueSource(strings = {"<red>text", "<color:red>text", "<colour:red>text", "<c:red>text"})
    void testColorIsResolvedByGroupNode(String input) {
        assertResolvedTo(serializer(TestPermissions.set(GROUP_NODE)), input, RED_TEXT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"<red>text", "<color:red>text", "<colour:red>text", "<c:red>text"})
    void testColorIsLeftAsTypedWithoutPermission(String input) {
        assertLeftAsTyped(serializer(TestPermissions.set()), input);
    }

    /* The node of an individual color */

    @Test
    void testColorIsResolvedByItsOwnNode() {
        MiniMessage serializer = serializer(TestPermissions.set(RED_NODE));

        assertResolvedTo(serializer, "<red>text", RED_TEXT);
        assertLeftAsTyped(serializer, "<blue>text");
    }

    @ParameterizedTest
    @CsvSource(RED_INPUTS)
    void testDeniedColorNodeOverridesGroupNode(String input) {
        MiniMessage serializer = serializer(TestPermissions.set(GROUP_NODE, "-" + RED_NODE));

        assertLeftAsTyped(serializer, input);
        assertResolvedTo(serializer, "<blue>text", Component.text("text", NamedTextColor.BLUE));
    }

    @Test
    void testTagNameIsCaseInsensitive() {
        assertResolvedTo(serializer(TestPermissions.set(GROUP_NODE)), "<RED>text", RED_TEXT);
        assertLeftAsTyped(serializer(TestPermissions.set(GROUP_NODE, "-" + RED_NODE)), "<RED>text");
    }

    /* Hexadecimal colors, which share a single node */

    @ParameterizedTest
    @ValueSource(strings = {"<#ff0000>text", "<color:#ff0000>text"})
    void testHexColorIsCheckedAgainstTheHexNode(String input) {
        Component expected = Component.text("text", TextColor.color(0xff0000));

        assertResolvedTo(serializer(TestPermissions.set(GROUP_NODE)), input, expected);
        assertResolvedTo(serializer(TestPermissions.set(HEX_NODE)), input, expected);
        assertLeftAsTyped(serializer(TestPermissions.set(GROUP_NODE, "-" + HEX_NODE)), input);
    }

    @Test
    void testHexNodeDoesNotAffectNamedColors() {
        assertLeftAsTyped(serializer(TestPermissions.set(HEX_NODE)), "<red>text");
    }

    /* The color names MiniMessage accepts but NamedTextColor does not know about */

    @ParameterizedTest
    @CsvSource({"<grey>text, gray", "<color:grey>text, gray", "<dark_grey>text, dark_gray"})
    void testAliasIsCheckedAgainstTheNodeOfItsCanonicalName(String input, String canonicalName) {
        String canonicalNode = GROUP_NODE + "." + canonicalName;

        assertResolved(serializer(TestPermissions.set(canonicalNode)), input);
        assertLeftAsTyped(serializer(TestPermissions.set(GROUP_NODE, "-" + canonicalNode)), input);

        // An alias must not fall through to the node of a hexadecimal color.
        assertLeftAsTyped(serializer(TestPermissions.set(HEX_NODE)), input);
    }

    /* Parsing */

    @Test
    void testTagWithoutArgumentIsLeftAsTyped() {
        assertLeftAsTyped(serializer(TestPermissions.set(GROUP_NODE)), "<color>text");
    }

    @Test
    void testClosingTagIsResolved() {
        assertResolvedTo(
            serializer(TestPermissions.set(GROUP_NODE)),
            "<red>red</red>plain",
            Component.text("").append(Component.text("red", NamedTextColor.RED)).append(Component.text("plain"))
        );
    }

    /* When the permission is read */

    @Test
    void testPermissionIsCheckedOnEveryUse() {
        Map<String, TriState> states = new HashMap<>();
        MiniMessage serializer = serializer(TestPermissions.backedBy(states));

        assertLeftAsTyped(serializer, "<red>text");

        states.put(GROUP_NODE, TriState.TRUE);

        assertResolvedTo(serializer, "<red>text", RED_TEXT);
    }

    private static MiniMessage serializer(PermissionChecker checker) {
        return MiniMessage.builder().tags(ColorTagResolver.create(checker, GROUP_NODE)).build();
    }
}
