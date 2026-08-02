package net.okocraft.yaminabe.common.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Assertions;

/**
 * Assertions on what a permission-aware {@link MiniMessage} does with an input.
 * <p>
 * A tag that is not allowed is left as the text it was typed as, so "resolved" and "left as typed" are the two
 * outcomes every test here is about.
 */
final class MiniMessageAssertions {

    /**
     * Asserts that the given input is deserialized into the given {@link Component}.
     */
    static void assertResolvedTo(MiniMessage serializer, String input, Component expected) {
        Assertions.assertEquals(expected, serializer.deserialize(input), input);
    }

    /**
     * Asserts that the tags in the given input are resolved, whatever they are resolved into.
     * <p>
     * This is for the inputs whose expected {@link Component} says nothing a reader would not already know, such as
     * one tag per {@link FormatTag}.
     */
    static void assertResolved(MiniMessage serializer, String input) {
        Assertions.assertNotEquals(Component.text(input), serializer.deserialize(input), input);
    }

    /**
     * Asserts that the given input is deserialized into exactly the text it was typed as.
     */
    static void assertLeftAsTyped(MiniMessage serializer, String input) {
        Assertions.assertEquals(Component.text(input), serializer.deserialize(input), input);
    }

    private MiniMessageAssertions() {
        throw new UnsupportedOperationException();
    }
}
