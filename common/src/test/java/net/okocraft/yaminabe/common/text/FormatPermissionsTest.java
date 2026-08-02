package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.util.TriState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertLeftAsTyped;
import static net.okocraft.yaminabe.common.text.MiniMessageAssertions.assertResolved;

class FormatPermissionsTest {

    private static final String BASE = "test.format";

    /**
     * One representative input per {@link FormatTag}, so that a wrong permission name or a wrong tag name is caught.
     */
    private static final Map<FormatTag, String> INPUTS = createInputs();

    private static Map<FormatTag, String> createInputs() {
        Map<FormatTag, String> inputs = new EnumMap<>(FormatTag.class);

        inputs.put(FormatTag.COLOR, "<red>text");
        inputs.put(FormatTag.DECORATION, "<bold>text");
        inputs.put(FormatTag.GRADIENT, "<gradient:red:blue>text</gradient>");
        inputs.put(FormatTag.RAINBOW, "<rainbow>text</rainbow>");
        inputs.put(FormatTag.TRANSITION, "<transition:red:blue:0.5>text</transition>");
        inputs.put(FormatTag.SHADOW_COLOR, "<shadow:#ff0000ff>text");
        inputs.put(FormatTag.FONT, "<font:uniform>text");
        inputs.put(FormatTag.CLICK, "<click:run_command:'/test'>text");
        inputs.put(FormatTag.HOVER, "<hover:show_text:'hover'>text");
        inputs.put(FormatTag.INSERTION, "<insert:text>text");
        inputs.put(FormatTag.TRANSLATABLE, "<lang:test.key>");
        inputs.put(FormatTag.RESET, "<reset>text");
        inputs.put(FormatTag.NEWLINE, "<newline>text");

        return inputs;
    }

    /* Every group, offered on its own */

    @ParameterizedTest
    @EnumSource(FormatTag.class)
    void testTagIsResolvedByItsOwnGroupNode(FormatTag tag) {
        String input = input(tag);

        assertResolved(serializer(TestPermissions.set(BASE + "." + tag.permissionName()), EnumSet.of(tag)), input);
        assertLeftAsTyped(serializer(TestPermissions.set(), EnumSet.of(tag)), input);
    }

    @ParameterizedTest
    @EnumSource(FormatTag.class)
    void testTagIsNeverResolvedWhenItIsNotOffered(FormatTag tag) {
        Set<FormatTag> others = EnumSet.complementOf(EnumSet.of(tag));

        // Every permission is granted, but the caller does not offer this group.
        assertLeftAsTyped(serializer(TestPermissions.all(), others), input(tag));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<pride>text</pride>", "<nbt:block:'0 0 0':'Items'>", "<score:player:objective>", "<sel:@p>", "<sprite:x:y>"})
    void testTagWithoutAGroupIsNeverResolved(String input) {
        // These tags have no FormatTag, so no permission can enable them.
        assertLeftAsTyped(serializer(TestPermissions.all(), EnumSet.allOf(FormatTag.class)), input);
    }

    /* When the permission is read */

    @Test
    void testGroupPermissionIsCheckedOnEveryUse() {
        Map<String, TriState> states = new HashMap<>();
        MiniMessage serializer = serializer(TestPermissions.backedBy(states), EnumSet.of(FormatTag.CLICK));
        String input = input(FormatTag.CLICK);

        assertLeftAsTyped(serializer, input);

        states.put(BASE + "." + FormatTag.CLICK.permissionName(), TriState.TRUE);

        assertResolved(serializer, input);

        states.put(BASE + "." + FormatTag.CLICK.permissionName(), TriState.FALSE);

        assertLeftAsTyped(serializer, input);
    }

    /* The groups themselves */

    @Test
    void testPermissionNamesAreUnique() {
        Assertions.assertEquals(
            FormatTag.values().length,
            EnumSet.allOf(FormatTag.class).stream().map(FormatTag::permissionName).distinct().count()
        );
    }

    private static String input(FormatTag tag) {
        String input = INPUTS.get(tag);
        Assertions.assertNotNull(input, "no input is defined for " + tag);
        return input;
    }

    private static MiniMessage serializer(PermissionChecker checker, Set<FormatTag> tags) {
        return FormatPermissions.createSerializer(checker, BASE, tags);
    }
}
