package net.okocraft.yaminabe.paper.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.ComponentLike;
import net.okocraft.yaminabe.common.player.PlayerProfile;
import net.okocraft.yaminabe.common.player.PlayerProfileRepository;
import net.okocraft.yaminabe.common.player.PlayerProfileService;
import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SeenCommandTest {

    private static final String PERMISSION = "yaminabe.command.seen";
    private static final Instant JOIN = Instant.parse("2026-10-04T11:00:00Z");
    private final PlayerProfileRepository repository = mock(PlayerProfileRepository.class);
    private final PlayerProfileService service = new PlayerProfileService(this.repository, Clock.systemUTC());
    private final EntityScheduler scheduler = mock(EntityScheduler.class);
    private final CommandSender sender = mock(CommandSender.class);

    @Test
    void canonicalUuidUsesUuidLookupAndConfirmedDeparture() throws Exception {
        TestSources.grant(this.sender, PERMISSION);
        UUID uuid = UUID.randomUUID();
        var profile = new PlayerProfile(uuid, "Example", JOIN, JOIN.plusSeconds(20), null);
        when(this.repository.find(uuid)).thenReturn(CompletableFuture.completedFuture(Optional.of(profile)));
        assertEquals(1, this.tester().execute(TestSources.ofSenderOnly(this.sender), "seen " + uuid));
        verify(this.sender).sendMessage(CommandMessages.SEEN_OFFLINE.apply("Example", JOIN.plusSeconds(20).toString()));
        verify(this.repository, never()).findByName(anyString());
    }

    @Test
    void unknownNameDoesNotCreateProfile() throws Exception {
        TestSources.grant(this.sender, PERMISSION);
        when(this.repository.findByName("Nobody")).thenReturn(CompletableFuture.completedFuture(List.of()));
        this.tester().execute(TestSources.ofSenderOnly(this.sender), "seen Nobody");
        verify(this.sender).sendMessage(CommandMessages.SEEN_NOT_FOUND.apply("Nobody"));
        verify(this.repository, never()).recordJoin(any(), anyString(), any(), any());
    }

    @Test
    void ambiguousNameListsUuidsWithoutSelectingOne() throws Exception {
        TestSources.grant(this.sender, PERMISSION);
        var first = profile(UUID.randomUUID());
        var second = profile(UUID.randomUUID());
        when(this.repository.findByName("Example")).thenReturn(CompletableFuture.completedFuture(List.of(first, second)));
        this.tester().execute(TestSources.ofSenderOnly(this.sender), "seen Example");
        verify(this.sender).sendMessage(CommandMessages.SEEN_AMBIGUOUS.apply("Example"));
        verify(this.sender).sendMessage(CommandMessages.SEEN_IDENTITY.apply("Example", first.uuid().toString()));
        verify(this.sender).sendMessage(CommandMessages.SEEN_IDENTITY.apply("Example", second.uuid().toString()));
        verifyNoMoreInteractions(ignoreStubs(this.sender));
    }

    @Test
    void previousUnclosedSessionIsNotReportedOnline() throws Exception {
        TestSources.grant(this.sender, PERMISSION);
        when(this.repository.findByName("Example")).thenReturn(CompletableFuture.completedFuture(List.of(profile(UUID.randomUUID()))));
        this.tester().execute(TestSources.ofSenderOnly(this.sender), "seen Example");
        verify(this.sender).sendMessage(CommandMessages.SEEN_UNCONFIRMED.apply("Example", JOIN.toString()));
    }

    @Test
    void failureIsReportedInsteadOfNotFound() throws Exception {
        TestSources.grant(this.sender, PERMISSION);
        when(this.repository.findByName("Example")).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("disk failure")));
        this.tester().execute(TestSources.ofSenderOnly(this.sender), "seen Example");
        verify(this.sender).sendMessage(CommandMessages.SEEN_FAILED);
        verify(this.sender, never()).sendMessage(CommandMessages.SEEN_NOT_FOUND.apply("Example"));
    }

    @Test
    void commandChecksSenderRatherThanExecuteContext() {
        Player executor = mock(Player.class);
        TestSources.grant(executor, PERMISSION);
        assertThrows(CommandSyntaxException.class, () -> this.tester().execute(TestSources.of(this.sender, executor), "seen Example"));
        verifyNoInteractions(this.repository);
    }

    @Test
    void lostPermissionWhileLookupIsPendingSuppressesReply() throws Exception {
        TestSources.grant(this.sender, PERMISSION);
        var pending = new CompletableFuture<List<PlayerProfile>>();
        when(this.repository.findByName("Example")).thenReturn(pending);
        this.tester().execute(TestSources.ofSenderOnly(this.sender), "seen Example");
        TestSources.deny(this.sender, PERMISSION);
        pending.complete(List.of(profile(UUID.randomUUID())));
        verify(this.sender, never()).sendMessage(any(ComponentLike.class));
    }

    @Test
    void replyRunsOnSendersEntityThreadAndDiscardsDepartedConnection() throws Exception {
        Player player = mock(Player.class);
        TestSources.grant(player, PERMISSION);
        var action = new AtomicReference<Runnable>();
        when(this.scheduler.execute(eq(player), any(), any())).thenAnswer(invocation -> {
            action.set(invocation.getArgument(1));
            return true;
        });
        when(this.repository.findByName("Example")).thenReturn(CompletableFuture.completedFuture(List.of()));
        this.tester().execute(TestSources.of(player), "seen Example");
        verify(player, never()).sendMessage(any(ComponentLike.class));
        assertNotNull(action.get());
        action.get().run(); // isOnline is false: the old connection does not receive a response.
        verify(player, never()).sendMessage(any(ComponentLike.class));
    }

    @Test
    void hiddenTargetIsNotReportedOnline() throws Exception {
        Player viewer = mock(Player.class);
        Player target = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        TestSources.grant(viewer, PERMISSION);
        when(viewer.isOnline()).thenReturn(true);
        when(this.repository.recordJoin(eq(uuid), anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(profile(uuid)));
        this.service.join(uuid, "Example");
        when(this.repository.findByName("Example")).thenReturn(CompletableFuture.completedFuture(List.of(profile(uuid))));
        when(this.scheduler.execute(eq(viewer), any(), any())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return true;
        });
        this.tester(ignored -> target).execute(TestSources.of(viewer), "seen Example");
        verify(viewer).sendMessage(CommandMessages.SEEN_UNCONFIRMED.apply("Example", JOIN.toString()));
        verify(viewer, never()).sendMessage(CommandMessages.SEEN_ONLINE.apply("Example", JOIN.toString()));
    }

    private CommandTester tester() {
        return this.tester(ignored -> null);
    }

    private CommandTester tester(Function<UUID, Player> online) {
        return CommandTester.of(SeenCommand.createSeenCommand(this.service, this.scheduler, online));
    }

    private static PlayerProfile profile(UUID uuid) {
        return new PlayerProfile(uuid, "Example", JOIN, null, UUID.randomUUID());
    }
}
