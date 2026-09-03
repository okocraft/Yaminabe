package net.okocraft.yaminabe.paper.platform;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

class PaperEntityScheduler implements EntityScheduler {

    private final Plugin plugin;

    PaperEntityScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull Entity entity, @NotNull Runnable task) {
        if (this.plugin.getServer().isOwnedByCurrentRegion(entity)) {
            task.run();
            return true;
        }

        return entity.getScheduler().execute(this.plugin, task, null, 1);
    }
}
