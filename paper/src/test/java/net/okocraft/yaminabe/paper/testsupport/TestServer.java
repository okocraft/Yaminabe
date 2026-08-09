package net.okocraft.yaminabe.paper.testsupport;

import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.flag.FeatureFlags;
import org.bukkit.craftbukkit.CraftRegistry;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Brings up as much of a server as items and commands need, so that a test works with the real registries, the real
 * item stacks and the real argument types instead of with stand-ins for them.
 * <p>
 * No world, no network and no {@link org.bukkit.Server} are started: what is set up is the data a server loads before
 * any of that, which is what an item is made of.
 */
public final class TestServer {

    private static boolean setUp;

    /**
     * Sets the server up, or returns right away if it is already set up.
     * <p>
     * The setup can only be done once in a JVM, as the registries a server holds are static.
     */
    public static synchronized void setUp() {
        if (setUp) {
            return;
        }

        setUp = true;

        // Bootstrapping replaces the standard streams with ones that log through slf4j, so the logger has to hold on
        // to the original streams beforehand, which it does on its first use. See the test task in build.gradle.kts.
        LoggerFactory.getLogger(TestServer.class).info("Setting the test server up.");

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        RegistryAccess.Frozen registries = loadRegistries();

        // An item does not carry its own default data components; they are built once from the loaded registries.
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(DataComponentInitializers.PendingComponents::apply);

        CraftRegistry.setMinecraftRegistry(registries);
        setUpArgumentTypes(registries);
    }

    private static RegistryAccess.Frozen loadRegistries() {
        PackRepository packs = ServerPacksSource.createVanillaTrustedRepository();
        packs.reload();
        packs.setSelected(packs.getAvailableIds(), false);

        ResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, packs.openAllSelected());

        // A data-driven registry, such as the enchantment one, refers to the tags of the built-in registries, so those
        // have to be loaded before it and are then applied to the registries the loaded one refers to.
        List<Registry.PendingTags<?>> tags = TagLoader.loadTagsForExistingRegistries(resources, RegistryLayer.STATIC_ACCESS);
        tags.forEach(Registry.PendingTags::apply);

        RegistryAccess.Frozen loaded = RegistryDataLoader.load(
            resources,
            TagLoader.buildUpdatedLookups(RegistryLayer.STATIC_ACCESS, tags),
            RegistryDataLoader.WORLDGEN_REGISTRIES,
            Runnable::run
        ).join();

        LayeredRegistryAccess<RegistryLayer> layers = RegistryLayer.createRegistryAccess().replaceFrom(RegistryLayer.WORLDGEN, loaded);
        return layers.compositeAccess().freeze();
    }

    private static void setUpArgumentTypes(RegistryAccess.Frozen registries) {
        // An argument type of ArgumentTypes is built from the context a server hands to its command dispatcher.
        CommandBuildContext context = CommandBuildContext.simple(registries, FeatureFlags.REGISTRY.allFlags());
        io.papermc.paper.command.brigadier.PaperCommands.INSTANCE.setDispatcher(new Commands(Commands.CommandSelection.ALL, context), context);
    }

    private TestServer() {
        throw new UnsupportedOperationException();
    }
}
