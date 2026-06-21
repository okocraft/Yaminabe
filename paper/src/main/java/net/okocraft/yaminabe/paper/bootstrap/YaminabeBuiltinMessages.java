package net.okocraft.yaminabe.paper.bootstrap;

import dev.siroshun.mcmsgdef.DefaultMessageDefiner;
import net.okocraft.yaminabe.paper.command.YaminabeCommands;

import java.util.List;

final class YaminabeBuiltinMessages {

    static final List<DefaultMessageDefiner> DEFINERS = List.of(
        YaminabeCommands.getDefiner()
    );

    private YaminabeBuiltinMessages() {
        throw new UnsupportedOperationException();
    }
}
