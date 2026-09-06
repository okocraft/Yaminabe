package net.okocraft.yaminabe.velocity.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class YaminabeVelocityConfigTest {

    @Test
    void testHolderHasConfigBeforeReload(@TempDir Path dir) {
        Assertions.assertNotNull(new YaminabeVelocityConfig.Holder(dir).get(), "the holder must not expose null");
    }

    @Test
    void testReloadRoundTrip(@TempDir Path dir) throws Exception {
        var holder = new YaminabeVelocityConfig.Holder(dir);

        holder.reload();

        Assertions.assertTrue(Files.exists(dir.resolve("config.yml")));
        Assertions.assertDoesNotThrow(() -> new YaminabeVelocityConfig.Holder(dir).reload());
    }

    @Test
    void testReloadReadsDebugSetting(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), "debug: true\n");

        var holder = new YaminabeVelocityConfig.Holder(dir);
        holder.reload();

        Assertions.assertTrue(holder.get().debug());
    }

    @Test
    void testRestartDefaultsToSupervisorMode(@TempDir Path dir) throws Exception {
        var holder = new YaminabeVelocityConfig.Holder(dir);
        holder.reload();

        var restart = holder.get().restart();
        Assertions.assertEquals(YaminabeVelocityConfig.RestartMode.SUPERVISOR, restart.mode());
        Assertions.assertTrue(restart.command().isEmpty());
        Assertions.assertEquals(60, restart.defaultCountdownSeconds());
        Assertions.assertEquals("", restart.timeZone());
        Assertions.assertTrue(restart.beforeRestart().commands().isEmpty());
        Assertions.assertTrue(restart.beforeRestart().kickPlayers());
        Assertions.assertTrue(restart.beforeShutdown().commands().isEmpty());
        Assertions.assertTrue(restart.beforeShutdown().kickPlayers());
    }

    @Test
    void testReloadReadsRestartSettings(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("config.yml"), """
            restart:
              mode: COMMAND
              command:
                - java
                - -jar
                - velocity.jar
              default-countdown-seconds: 30
              time-zone: Asia/Tokyo
              before-restart:
                commands:
                  - alert restarting
                kick-players: false
              before-shutdown:
                commands:
                  - alert stopping
                kick-players: true
            """);

        var holder = new YaminabeVelocityConfig.Holder(dir);
        holder.reload();

        var restart = holder.get().restart();
        Assertions.assertEquals(YaminabeVelocityConfig.RestartMode.COMMAND, restart.mode());
        Assertions.assertEquals(List.of("java", "-jar", "velocity.jar"), restart.command());
        Assertions.assertEquals(30, restart.defaultCountdownSeconds());
        Assertions.assertEquals("Asia/Tokyo", restart.timeZone());
        Assertions.assertEquals(List.of("alert restarting"), restart.beforeRestart().commands());
        Assertions.assertFalse(restart.beforeRestart().kickPlayers());
        Assertions.assertEquals(List.of("alert stopping"), restart.beforeShutdown().commands());
        Assertions.assertTrue(restart.beforeShutdown().kickPlayers());
    }

    @Test
    void testReloadSwapsHeldConfig(@TempDir Path dir) throws Exception {
        var holder = new YaminabeVelocityConfig.Holder(dir);
        var before = holder.get();

        holder.reload();

        Assertions.assertNotSame(before, holder.get());
    }

    @Test
    void testFailedReloadKeepsHeldConfig(@TempDir Path dir) throws Exception {
        var holder = new YaminabeVelocityConfig.Holder(dir);
        holder.reload();
        var loaded = holder.get();

        Files.writeString(dir.resolve("config.yml"), "broken: [\n");

        Assertions.assertThrows(ConfigurateException.class, holder::reload);
        Assertions.assertSame(loaded, holder.get(), "the held config must survive a failed reload");
    }

    @Test
    void testHolderRejectsNullDirectory() {
        Assertions.assertThrows(NullPointerException.class, () -> new YaminabeVelocityConfig.Holder(null));
    }
}
