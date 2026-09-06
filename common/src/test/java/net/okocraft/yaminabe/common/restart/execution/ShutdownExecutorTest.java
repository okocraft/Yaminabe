package net.okocraft.yaminabe.common.restart.execution;

import net.kyori.adventure.text.Component;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class ShutdownExecutorTest {

    @Test
    void testCommandsAreExecutedSequentiallyBeforeKickAndRestart() {
        TestController controller = new TestController();
        CompletableFuture<Boolean> first = new CompletableFuture<>();
        CompletableFuture<Boolean> second = new CompletableFuture<>();
        controller.commandResults.add(first);
        controller.commandResults.add(second);

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

        new ShutdownExecutor(controller).execute(
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
        controller.throwOnKick = true;

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

    private static final class TestController implements ServerController {
        private final List<String> events = new ArrayList<>();
        private final List<CompletableFuture<Boolean>> commandResults = new ArrayList<>();
        private int commandIndex;
        private boolean throwOnKick;

        @Override
        public CompletionStage<Boolean> dispatchConsoleCommand(String command) {
            this.events.add("command:" + command);
            return this.commandResults.get(this.commandIndex++);
        }

        @Override
        public void kickAll(Component reason) {
            this.events.add("kick");
            if (this.throwOnKick) {
                throw new IllegalStateException("kick failed");
            }
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
