package net.okocraft.yaminabe.common.restart.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.common.platform.scheduler.CancellableTask;
import net.okocraft.yaminabe.common.platform.scheduler.Scheduler;
import net.okocraft.yaminabe.common.restart.RestartService;
import net.okocraft.yaminabe.common.restart.ShutdownReservation;
import net.okocraft.yaminabe.common.restart.ShutdownType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class RestartNowCommandTest {

    private static final Instant NOW = Instant.parse("2026-09-06T03:00:00Z");
    private static final RestartCommandSettings SETTINGS = new RestartCommandSettings(
        Duration.ofSeconds(60),
        ZoneId.of("Asia/Tokyo")
    );

    @Test
    void testImmediateRestart() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(1, fixture.dispatcher.execute("vrestart", fixture.source));

        ShutdownReservation reservation = fixture.service.current().orElseThrow().reservation();
        Assertions.assertEquals(ShutdownType.RESTART, reservation.type());
        Assertions.assertEquals(NOW, reservation.executeAt());
        Assertions.assertEquals(NOW, reservation.countdownStartAt());
        Assertions.assertNull(reservation.reason());
    }

    @Test
    void testImmediateRestartWithReason() throws Exception {
        Fixture fixture = fixture();
        fixture.source.permissions.add(RestartCommandPermissions.RESTART);

        Assertions.assertEquals(
            1,
            fixture.dispatcher.execute("vrestart reason proxy maintenance", fixture.source)
        );

        Assertions.assertEquals(
            "proxy maintenance",
            fixture.service.current().orElseThrow().reservation().reason()
        );
    }

    @Test
    void testCommandIsHiddenWithoutRestartPermission() {
        Fixture fixture = fixture();

        Assertions.assertThrows(CommandSyntaxException.class, () -> fixture.dispatcher.execute("vrestart", fixture.source));
        Assertions.assertTrue(fixture.service.current().isEmpty());
    }

    private static Fixture fixture() {
        TestScheduler scheduler = new TestScheduler();
        RestartService service = new RestartService(
            scheduler,
            Clock.fixed(NOW, ZoneOffset.UTC),
            new RestartService.Listener() {
            }
        );
        TestSource source = new TestSource();
        RestartCommandSource<TestSource> adapter = new RestartCommandSource<>() {
            @Override
            public boolean hasPermission(TestSource commandSource, String permission) {
                return commandSource.permissions.contains(permission);
            }

            @Override
            public void sendMessage(TestSource commandSource, ComponentLike message) {
                commandSource.messages.add(message);
            }
        };
        CommandDispatcher<TestSource> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(RestartNowCommand.create(
            "vrestart",
            service,
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> SETTINGS,
            adapter
        ));
        return new Fixture(dispatcher, service, source);
    }

    private record Fixture(CommandDispatcher<TestSource> dispatcher, RestartService service, TestSource source) {
    }

    private static final class TestSource {
        private final Set<String> permissions = new HashSet<>();
        private final List<ComponentLike> messages = new ArrayList<>();
    }

    private static final class TestScheduler implements Scheduler {
        @Override
        public void runNow(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CancellableTask runDelayed(@NotNull Runnable task, @NotNull Duration delay) {
            return () -> {
            };
        }

        @Override
        public @NotNull CancellableTask runAtFixedRate(
            @NotNull Consumer<CancellableTask> task,
            @NotNull Duration interval
        ) {
            throw new UnsupportedOperationException();
        }
    }
}
