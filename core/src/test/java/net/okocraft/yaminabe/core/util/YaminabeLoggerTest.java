package net.okocraft.yaminabe.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.helpers.SubstituteLogger;

import java.util.concurrent.atomic.AtomicReference;

class YaminabeLoggerTest {

    @Test
    void testLogAndDebug() {
        var lastLog = new AtomicReference<String>();

        var logger = Mockito.mock(Logger.class);
        Mockito.doAnswer(invocation -> {
            lastLog.set(invocation.getArgument(0));
            return null;
        }).when(logger).info(Mockito.anyString());

        ((SubstituteLogger) YaminabeLogger.log()).setDelegate(logger);

        Assertions.assertNull(lastLog.get());

        /* Normal logging */
        YaminabeLogger.log().info("First info log");
        Assertions.assertEquals("First info log", lastLog.getAndSet(null));

        /* Debug Logging */

        // Default state (Disabled)
        YaminabeLogger.debug().info("First debug log (will not be printed)");
        Assertions.assertNull(lastLog.get());

        // Enabled
        YaminabeLogger.logDebug(true);
        YaminabeLogger.debug().info("Second debug log (will be printed)");
        Assertions.assertEquals("Second debug log (will be printed)", lastLog.getAndSet(null));

        // Disabled
        YaminabeLogger.logDebug(false);
        YaminabeLogger.debug().info("Third debug log (will not be printed)");
        Assertions.assertNull(lastLog.get());
    }
}
