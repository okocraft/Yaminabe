package net.okocraft.paper.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import net.okocraft.paper.plugin.YaminabePlugin;
import net.okocraft.yaminabe.common.PluginStatus;
import net.okocraft.yaminabe.common.YaminabeLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.SubstituteLogger;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class YaminabeBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        ((SubstituteLogger) YaminabeLogger.log()).setDelegate(context.getLogger());
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new YaminabePlugin(PluginStatus.NOT_LOADED);
    }
}
