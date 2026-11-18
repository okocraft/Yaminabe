package net.okocraft.yaminabe.paper.listener;

import net.okocraft.yaminabe.common.player.PlayerProfile;
import net.okocraft.yaminabe.common.player.PlayerProfileRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class PlayerProfileListenerTest {

    @Test
    void capturesIdentityAndUpdateTimeBeforeAsynchronousPersistence() {
        UUID uuid = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-05T00:00:00Z");
        var profile = new PlayerProfile(uuid, "Example", at);
        var repository = mock(PlayerProfileRepository.class);
        var pending = new CompletableFuture<Void>();
        when(repository.upsert(profile)).thenReturn(pending);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("Example");
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.empty());
        new PlayerProfileListener(repository, Clock.fixed(at, ZoneOffset.UTC)).onJoin(event);
        verify(repository).upsert(profile);
        pending.complete(null);
        verify(player).getUniqueId();
        verify(player).getName();
        verifyNoMoreInteractions(ignoreStubs(player));
    }
}
