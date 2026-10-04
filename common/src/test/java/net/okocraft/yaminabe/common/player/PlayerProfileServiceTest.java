package net.okocraft.yaminabe.common.player;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlayerProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-04T11:00:00Z");

    @Test
    void reconnectRejectsOldQuitEvenWhileInitialLoadIsPending() {
        var repository = mock(PlayerProfileRepository.class);
        when(repository.recordJoin(any(), anyString(), any(), any())).thenReturn(new CompletableFuture<>());
        when(repository.recordQuit(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
        var service = new PlayerProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        UUID uuid = UUID.randomUUID();
        var first = service.join(uuid, "Example");
        var second = service.join(uuid, "Example");
        assertFalse(first.ready().isDone());
        assertFalse(service.quit(first).join());
        assertTrue(service.isOnline(uuid));
        verify(repository, never()).recordQuit(eq(uuid), eq(first.sessionId()), any());
        assertTrue(service.quit(second).join());
        assertFalse(service.isOnline(uuid));
    }

    @Test
    void shutdownRecordsActiveDeparturesBeforeClosingStorage() {
        var repository = mock(PlayerProfileRepository.class);
        when(repository.recordJoin(any(), anyString(), any(), any())).thenReturn(new CompletableFuture<>());
        when(repository.recordQuit(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
        when(repository.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        var service = new PlayerProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        var session = service.join(UUID.randomUUID(), "Example");
        var closing = service.closeAsync();
        closing.join();
        assertSame(closing, service.closeAsync());
        assertFalse(service.isOnline(session.playerId()));
        assertFalse(service.quit(session).join());
        assertThrows(IllegalStateException.class, () -> service.join(session.playerId(), "Example"));
        var order = inOrder(repository);
        order.verify(repository).recordJoin(session.playerId(), "Example", session.sessionId(), NOW);
        order.verify(repository).recordQuit(session.playerId(), session.sessionId(), NOW);
        order.verify(repository).closeAsync();
    }

    @Test
    void failedDepartureMakesShutdownExceptional() {
        var repository = mock(PlayerProfileRepository.class);
        when(repository.recordJoin(any(), anyString(), any(), any())).thenReturn(new CompletableFuture<>());
        when(repository.recordQuit(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("disk failure")));
        when(repository.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        var service = new PlayerProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        service.join(UUID.randomUUID(), "Example");
        assertThrows(java.util.concurrent.CompletionException.class, () -> service.closeAsync().join());
        verify(repository).closeAsync();
    }
}
