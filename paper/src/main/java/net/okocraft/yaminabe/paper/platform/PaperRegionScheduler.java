package net.okocraft.yaminabe.paper.platform;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

class PaperRegionScheduler implements RegionScheduler {

    private final Plugin plugin;

    PaperRegionScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull Location location, @NotNull Runnable task) {
        if (this.plugin.getServer().isOwnedByCurrentRegion(location)) {
            task.run();
            return true;
        }

        this.plugin.getServer().getRegionScheduler().execute(this.plugin, location, task);
        return false;
    }
}
