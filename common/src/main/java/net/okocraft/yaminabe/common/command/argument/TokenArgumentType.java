package net.okocraft.yaminabe.common.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;

public final class TokenArgumentType implements ArgumentType<String> {

    private static final TokenArgumentType INSTANCE = new TokenArgumentType();

    public static TokenArgumentType token() {
        return INSTANCE;
    }

    @Override
    public String parse(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private TokenArgumentType() {
    }
}
