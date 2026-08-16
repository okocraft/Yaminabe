package net.okocraft.yaminabe.common.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class ConfigLoaderTest {

    @ConfigSerializable
    static class TestConfig {
        @Comment("the name of something")
        String name = "default";
        int number = 10;
        Nested nested = new Nested();
        List<String> list = List.of("a", "b");
        Map<String, Integer> map = Map.of("k", 1);
    }

    @ConfigSerializable
    static class Nested {
        boolean enabled = true;
    }

    private static ConfigLoader<TestConfig> createLoader(Path filepath) {
        return new ConfigLoader<>(filepath, TestConfig.class, TestConfig::new);
    }

    @Test
    void testLoadFromExistingFile(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        Files.writeString(filepath, "name: loaded\nnumber: 42\n");

        var config = createLoader(filepath).load();

        Assertions.assertEquals("loaded", config.name);
        Assertions.assertEquals(42, config.number);
    }

    @Test
    void testLoadMissingFileWritesInitialConfig(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");

        var initial = new TestConfig();
        initial.name = "initial";
        initial.number = -1;

        var config = new ConfigLoader<>(filepath, TestConfig.class, () -> initial).load();

        Assertions.assertSame(initial, config);
        Assertions.assertTrue(Files.exists(filepath), "load() should create the file");

        // the written file must be readable back as the same config
        var reloaded = createLoader(filepath).load();
        Assertions.assertEquals("initial", reloaded.name);
        Assertions.assertEquals(-1, reloaded.number);
    }

    @Test
    void testGeneratedFileIsBlockStyle(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");

        createLoader(filepath).load();

        // @Comment is not written out: configurate-yaml 4.2.0 has no comment support
        Assertions.assertEquals(
            """
                name: default
                number: 10
                nested:
                  enabled: true
                list:
                - a
                - b
                map:
                  k: 1
                """,
            Files.readString(filepath)
        );
    }

    @Test
    void testLoadMissingFileCreatesParentDirectories(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("nested").resolve("dir").resolve("config.yml");

        createLoader(filepath).load();

        Assertions.assertTrue(Files.exists(filepath));
    }

    @Test
    void testLoadEmptyFileWritesInitialConfig(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        Files.writeString(filepath, "");

        var initial = new TestConfig();
        initial.name = "initial";

        var config = new ConfigLoader<>(filepath, TestConfig.class, () -> initial).load();

        Assertions.assertSame(initial, config);
        Assertions.assertEquals("initial", createLoader(filepath).load().name);
    }

    @Test
    void testLoadKeepsExistingFileUntouched(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        var content = "# comment\nname: loaded\n";
        Files.writeString(filepath, content);

        createLoader(filepath).load();

        Assertions.assertEquals(content, Files.readString(filepath), "an existing file must not be rewritten");
    }

    @Test
    void testLoadPartialFileUsesFieldDefaults(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        Files.writeString(filepath, "name: loaded\n");

        var initial = new TestConfig();
        initial.number = -1;

        var config = new ConfigLoader<>(filepath, TestConfig.class, () -> initial).load();

        Assertions.assertEquals("loaded", config.name);
        Assertions.assertEquals(10, config.number, "missing entries fall back to the field initializer, not to the initial config");
    }

    @Test
    void testLoadInvalidYamlThrows(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        Files.writeString(filepath, "name: [unclosed\n");

        Assertions.assertThrows(ConfigurateException.class, () -> createLoader(filepath).load());
    }

    @Test
    void testInitialConfigIsNotConsumedWhenFileHasValues(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        Files.writeString(filepath, "name: loaded\nnumber: 42\n");

        var counter = new AtomicInteger();
        var config = new ConfigLoader<>(filepath, TestConfig.class, () -> {
            counter.incrementAndGet();
            return new TestConfig();
        }).load();

        Assertions.assertEquals("loaded", config.name);
        Assertions.assertEquals(0, counter.get(), "the initial config should not be created when the file has values");
    }

    @Test
    void testLoadNonMapRootThrows(@TempDir Path dir) throws Exception {
        var scalarRoot = dir.resolve("scalar.yml");
        Files.writeString(scalarRoot, "hello\n");
        Assertions.assertThrows(ConfigurateException.class, () -> createLoader(scalarRoot).load());

        var listRoot = dir.resolve("list.yml");
        Files.writeString(listRoot, "- a\n- b\n");
        Assertions.assertThrows(ConfigurateException.class, () -> createLoader(listRoot).load());
    }

    @Test
    void testLoadCommentOnlyFileWritesInitialConfig(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        Files.writeString(filepath, "# user written comment\n");

        var initial = new TestConfig();
        initial.name = "initial";

        var config = new ConfigLoader<>(filepath, TestConfig.class, () -> initial).load();

        // a file holding no entries counts as empty, so the initial config is written into it
        Assertions.assertSame(initial, config);

        var written = Files.readString(filepath);
        Assertions.assertTrue(written.startsWith("# user written comment"), "the existing comment must be kept");
        Assertions.assertTrue(written.contains("name: initial"), written);
    }

    @Test
    void testLoadEmptyMapFileWritesInitialConfig(@TempDir Path dir) throws Exception {
        var filepath = dir.resolve("config.yml");
        Files.writeString(filepath, "{}\n");

        var initial = new TestConfig();
        initial.name = "initial";

        var config = new ConfigLoader<>(filepath, TestConfig.class, () -> initial).load();

        Assertions.assertSame(initial, config);
        Assertions.assertEquals("initial", createLoader(filepath).load().name);
    }

    @Test
    void testInitialConfigMustNotSupplyNull(@TempDir Path dir) {
        var filepath = dir.resolve("config.yml");

        Assertions.assertThrows(NullPointerException.class, () -> new ConfigLoader<TestConfig>(filepath, TestConfig.class, () -> null).load());
        Assertions.assertFalse(Files.exists(filepath), "nothing should be written when the initial config is null");
    }

    @Test
    void testConstructorRejectsNull(@TempDir Path dir) {
        var filepath = dir.resolve("config.yml");

        Assertions.assertThrows(NullPointerException.class, () -> new ConfigLoader<>(null, TestConfig.class, TestConfig::new));
        Assertions.assertThrows(NullPointerException.class, () -> new ConfigLoader<>(filepath, null, TestConfig::new));
        Assertions.assertThrows(NullPointerException.class, () -> new ConfigLoader<>(filepath, TestConfig.class, null));
    }
}
