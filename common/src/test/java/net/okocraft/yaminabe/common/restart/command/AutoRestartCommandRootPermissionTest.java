package net.okocraft.yaminabe.common.restart.command;

import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.RestartService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class AutoRestartCommandRootPermissionTest {

    private static final RestartCommandSource<TestSource> SOURCE_ADAPTER = new RestartCommandSource<>() {
        @Override
        public boolean hasPermission(TestSource source, String permission) {
            return source.permissions.contains(permission);
        }

        @Override
        public void sendMessage(TestSource source, ComponentLike message) {
        }
    };

    @Test
    void testRootRequiresAtLeastOneRestartPermission() {
        RestartService service = new RestartService(new UnusedScheduler(), new RestartService.Listener() {
        });
        var root = AutoRestartCommand.create(
            "autorestart",
            service,
            Clock.systemUTC(),
            () -> new RestartCommandSettings(Duration.ZERO, ZoneOffset.UTC),
            SOURCE_ADAPTER
        );
        TestSource source = new TestSource();

        Assertions.assertFalse(root.canUse(source));

        source.permissions.add(RestartCommandPermissions.RESTART);
        Assertions.assertTrue(root.canUse(source));
        source.permissions.clear();

        source.permissions.add(RestartCommandPermissions.STOP);
        Assertions.assertTrue(root.canUse(source));
        source.permissions.clear();

        source.permissions.add(RestartCommandPermissions.CANCEL);
        Assertions.assertTrue(root.canUse(source));
    }

    private static final class TestSource {
        private final Set<String> permissions = new HashSet<>();
    }

    private static final class UnusedScheduler implements Scheduler {

        @Override
        public void runNow(@NotNull Runnable task) {
            throw new AssertionError("scheduler must not be used");
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            throw new AssertionError("scheduler must not be used");
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(
            @NotNull Consumer<CancellableTask> task,
            @NotNull Duration interval
        ) {
            throw new AssertionError("scheduler must not be used");
        }
    }
}
