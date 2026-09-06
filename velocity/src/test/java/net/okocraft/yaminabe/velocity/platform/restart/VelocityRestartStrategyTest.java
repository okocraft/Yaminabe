package net.okocraft.yaminabe.velocity.platform.restart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class VelocityRestartStrategyTest {

    @Test
    void testCommandStrategyRegistersHookThatLaunchesConfiguredCommand() {
        AtomicReference<Thread> hook = new AtomicReference<>();
        List<List<String>> launched = new ArrayList<>();
        List<String> command = List.of("sh", "start.sh", "--port", "25577");
        var strategy = new CommandVelocityRestartStrategy(
            command,
            hook::set,
            launched::add
        );

        strategy.prepare();

        Assertions.assertNotNull(hook.get());
        Assertions.assertEquals("Yaminabe-Velocity-Restart", hook.get().getName());
        Assertions.assertTrue(launched.isEmpty());

        hook.get().run();
        Assertions.assertEquals(List.of(command), launched);
    }

    @Test
    void testCommandStrategyRejectsEmptyCommand() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> VelocityRestartStrategy.command(List.of())
        );
    }

    @Test
    void testSupervisorStrategyDoesNotRequirePreparation() {
        Assertions.assertDoesNotThrow(() -> VelocityRestartStrategy.supervisor().prepare());
    }
}
