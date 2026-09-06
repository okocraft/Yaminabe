package net.okocraft.yaminabe.velocity.platform.restart;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.ReservationSource;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import net.okocraft.yaminabe.common.restart.command.RestartCommandSettings;
import net.okocraft.yaminabe.common.restart.countdown.RestartCountdownSettings;
import net.okocraft.yaminabe.common.restart.execution.ServerController;
import net.okocraft.yaminabe.common.restart.execution.ShutdownExecutor;
import net.okocraft.yaminabe.velocity.config.VelocityRestartSettings;
import net.okocraft.yaminabe.velocity.config.YaminabeVelocityConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class VelocityShutdownExecutorTest {

    private static final Instant NOW = Instant.parse("2026-09-06T03:00:00Z");

    @Test
    void testRestartUsesRestartCommandsAndKicksPlayers() {
        TestController controller = new TestController();
        VelocityShutdownExecutor executor = new VelocityShutdownExecutor(
            new ShutdownExecutor(controller),
            VelocityShutdownExecutorTest::settings
        );

        executor.execute(reservation(ShutdownType.RESTART)).toCompletableFuture().join();

        Assertions.assertEquals(List.of("command:restart-command", "kick", "restart"), controller.events);
    }

    @Test
    void testStopUsesStopCommandsWithoutKick() {
        TestController controller = new TestController();
        VelocityShutdownExecutor executor = new VelocityShutdownExecutor(
            new ShutdownExecutor(controller),
            VelocityShutdownExecutorTest::settings
        );

        executor.execute(reservation(ShutdownType.STOP)).toCompletableFuture().join();

        Assertions.assertEquals(List.of("command:stop-command", "stop"), controller.events);
    }

    private static ShutdownReservation reservation(ShutdownType type) {
        return ShutdownReservation.create(
            NOW,
            NOW,
            Duration.ZERO,
            type,
            ReservationSource.MANUAL,
            "maintenance"
        );
    }

    private static VelocityRestartSettings settings() {
        return new VelocityRestartSettings(
            YaminabeVelocityConfig.RestartMode.SUPERVISOR,
            List.of(),
            new RestartCommandSettings(Duration.ZERO, ZoneOffset.UTC),
            Optional.empty(),
            new RestartCountdownSettings(false, BossBar.Color.RED, BossBar.Overlay.PROGRESS, Set.of()),
            new VelocityRestartSettings.ShutdownSettings(List.of("restart-command"), true),
            new VelocityRestartSettings.ShutdownSettings(List.of("stop-command"), false)
        );
    }

    private static final class TestController implements ServerController {
        private final List<String> events = new ArrayList<>();

        @Override
        public CompletionStage<Boolean> dispatchConsoleCommand(String command) {
            this.events.add("command:" + command);
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public void kickAll(Component reason) {
            this.events.add("kick");
        }

        @Override
        public void stop() {
            this.events.add("stop");
        }

        @Override
        public void restart() {
            this.events.add("restart");
        }
    }
}
