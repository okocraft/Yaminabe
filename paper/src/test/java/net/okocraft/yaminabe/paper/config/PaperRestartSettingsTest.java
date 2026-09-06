package net.okocraft.yaminabe.paper.config;

import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PaperRestartSettingsTest {

    @Test
    void testDefaultSettingsMapRestartAndStopSeparately() {
        PaperRestartSettings settings = PaperRestartSettings.from(new YaminabePaperConfig.Restart());

        Assertions.assertTrue(settings.before(ShutdownType.RESTART).commands().isEmpty());
        Assertions.assertTrue(settings.before(ShutdownType.RESTART).kickPlayers());
        Assertions.assertTrue(settings.before(ShutdownType.STOP).commands().isEmpty());
        Assertions.assertTrue(settings.before(ShutdownType.STOP).kickPlayers());
    }
}
