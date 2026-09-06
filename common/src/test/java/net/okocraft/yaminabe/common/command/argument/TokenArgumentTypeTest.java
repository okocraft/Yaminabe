package net.okocraft.yaminabe.common.command.argument;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TokenArgumentTypeTest {

    @Test
    void testReadsUntilWhitespace() {
        StringReader reader = new StringReader("18:00 countdown");

        Assertions.assertEquals("18:00", TokenArgumentType.token().parse(reader));
        Assertions.assertEquals(' ', reader.peek());
    }

    @Test
    void testAcceptsPunctuation() {
        StringReader reader = new StringReader("2026-09-10T18:00:30");

        Assertions.assertEquals("2026-09-10T18:00:30", TokenArgumentType.token().parse(reader));
        Assertions.assertFalse(reader.canRead());
    }
}
