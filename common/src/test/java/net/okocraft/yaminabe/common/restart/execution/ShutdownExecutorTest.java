package net.okocraft.yaminabe.common.restart.execution;

import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

class ShutdownExecutorTest {

    @Test
    void testCommandsAreExecutedSequentiallyBeforeKickAndRestart() {
        TestController controller = new TestController();
        CompletableFuture<Boolean> first = new CompletableFuture<>();
        CompletableFuture<Boolean> second = new CompletableFuture<>();
        CompletableFuture<Void> kick = new CompletableFuture<>();
        controller.commandResults.add(first);
        controller.commandResults.add(second);
        controller.kickResult = kick;

        CompletionStage<Void> execution = new ShutdownExecutor(controller).execute(
            ShutdownType.RESTART,
            List.of("first", "second"),
            Component.text("restart")
        );

        Assertions.assertEquals(List.of("command:first"), controller.events);
        Assertions.assertFalse(execution.toCompletableFuture().isDone());

        first.complete(true);
        Assertions.assertEquals(List.of("command:first", "command:second"), controller.events);
        Assertions.assertFalse(execution.toCompletableFuture().isDone());

        second.complete(true);
        Assertions.assertEquals(List.of("command:first", "command:second", "kick"), controller.events);
        Assertions.assertFalse(execution.toCompletableFuture().isDone());

        kick.complete(null);
        execution.toCompletableFuture().join();
        Assertions.assertEquals(
            List.of("command:first", "command:second", "kick", "restart"),
            controller.events
        );
    }

    @Test
    void testExecutionCanSkipPlayerKick() {
        TestController controller = new TestController();
        controller.commandResults.add(CompletableFuture.completedFuture(true));

        new ShutdownExecutor(controller).executeWithoutKick(
            ShutdownType.STOP,
            List.of("save")
        ).toCompletableFuture().join();

        Assertions.assertEquals(List.of("command:save", "stop"), controller.events);
    }

    @Test
    void testCommandAndKickFailuresDoNotPreventStop() {
        TestController controller = new TestController();
        CompletableFuture<Boolean> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("command failed"));
        controller.commandResults.add(failed);
        controller.commandResults.add(CompletableFuture.completedFuture(false));
        controller.kickResult = CompletableFuture.failedFuture(new IllegalStateException("kick failed"));

        new ShutdownExecutor(controller).execute(
            ShutdownType.STOP,
            List.of("first", "second"),
            Component.text("stop")
        ).toCompletableFuture().join();

        Assertions.assertEquals(
            List.of("command:first", "command:second", "kick", "stop"),
            controller.events
        );
    }

    @Test
    void testHungCommandCannotPreventStop() throws Exception {
        TestController controller = new TestController();
        controller.commandResults.add(new CompletableFuture<>());

        CompletionStage<Void> execution = new ShutdownExecutor(controller, Duration.ofMillis(25)).executeWithoutKick(
            ShutdownType.STOP,
            List.of("hung", "must-not-run")
        );

        execution.toCompletableFuture().get(1, TimeUnit.SECONDS);
        Assertions.assertEquals(List.of("command:hung", "stop"), controller.events);
    }

    @Test
    void testHungPlayerKickCannotPreventRestart() throws Exception {
        TestController controller = new TestController();
        controller.commandResults.add(CompletableFuture.completedFuture(true));
        controller.kickResult = new CompletableFuture<>();

        CompletionStage<Void> execution = new ShutdownExecutor(controller, Duration.ofMillis(25)).execute(
            ShutdownType.RESTART,
            List.of("save"),
            Component.text("restart")
        );

        execution.toCompletableFuture().get(1, TimeUnit.SECONDS);
        Assertions.assertEquals(List.of("command:save", "kick", "restart"), controller.events);
    }

    @Test
    void testSynchronousPreparationFailureFallsBackToTerminalActionOnce() {
        TestController controller = new TestController();

        new ShutdownExecutor(controller).executeWithoutKick(
            ShutdownType.STOP,
            Arrays.asList("save", null)
        ).toCompletableFuture().join();

        Assertions.assertEquals(List.of("stop"), controller.events);
    }

    private static final class TestController implements ServerController {
        private final List<String> events = new ArrayList<>();
        private final List<CompletableFuture<Boolean>> commandResults = new ArrayList<>();
        private int commandIndex;
        private CompletionStage<Void> kickResult = CompletableFuture.completedFuture(null);

        @Override
        public CompletionStage<Boolean> dispatchConsoleCommand(String command) {
            this.events.add("command:" + command);
            return this.commandResults.get(this.commandIndex++);
        }

        @Override
        public CompletionStage<Void> kickAll(Component reason) {
            this.events.add("kick");
            return this.kickResult;
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
