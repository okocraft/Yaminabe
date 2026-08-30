package net.okocraft.yaminabe.paper.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Files;
import java.nio.file.Path;

class YaminabePaperConfigTest {

    @Test
    void testHolderHasConfigBeforeReload(@TempDir Path dir) {
        Assertions.assertNotNull(new YaminabePaperConfig.Holder(dir).get(), "the holder must not expose null");
    }

    @Test
    void testReloadRoundTrip(@TempDir Path dir) throws Exception {
        var holder = new YaminabePaperConfig.Holder(dir);

        holder.reload(); // writes the initial config as the file does not exist yet

        Assertions.assertTrue(Files.exists(dir.resolve("config.yml")));

        Assertions.assertDoesNotThrow(() -> new YaminabePaperConfig.Holder(dir).reload());
    }

    @Test
    void testReloadSwapsHeldConfig(@TempDir Path dir) throws Exception {
        var holder = new YaminabePaperConfig.Holder(dir);
        var before = holder.get();

        holder.reload();

        Assertions.assertNotSame(before, holder.get());
    }

    @Test
    void testFailedReloadKeepsHeldConfig(@TempDir Path dir) throws Exception {
        var holder = new YaminabePaperConfig.Holder(dir);
        holder.reload();
        var loaded = holder.get();

        Files.writeString(dir.resolve("config.yml"), "broken: [\n");

        Assertions.assertThrows(ConfigurateException.class, holder::reload);
        Assertions.assertSame(loaded, holder.get(), "the held config must survive a failed reload");
    }

    @Test
    void testHolderRejectsNullDirectory() {
        Assertions.assertThrows(NullPointerException.class, () -> new YaminabePaperConfig.Holder(null));
    }
}
