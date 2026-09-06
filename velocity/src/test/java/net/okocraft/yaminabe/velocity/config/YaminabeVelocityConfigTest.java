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

        Assertions.assertEquals(YaminabeVelocityConfig.RestartMode.SUPERVISOR, holder.get().restart().mode());
        Assertions.assertTrue(holder.get().restart().command().isEmpty());
    }

    @Test
    void testReloadReadsCommandRestartSettings(@TempDir Path dir) throws Exception {
        Files.writeString(
            dir.resolve("config.yml"),
            "restart:\n  mode: COMMAND\n  command:\n    - sh\n    - start.sh\n    - --port\n    - '25577'\n"
        );

        var holder = new YaminabeVelocityConfig.Holder(dir);
        holder.reload();

        Assertions.assertEquals(YaminabeVelocityConfig.RestartMode.COMMAND, holder.get().restart().mode());
        Assertions.assertEquals(List.of("sh", "start.sh", "--port", "25577"), holder.get().restart().command());
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
