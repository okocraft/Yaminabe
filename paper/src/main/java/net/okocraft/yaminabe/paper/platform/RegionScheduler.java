package net.okocraft.yaminabe.paper.platform;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Runs a task on the thread that owns a location, which is the only thread that may touch the blocks and the entities
 * there while the server splits the world into regions, as Folia does.
 */
public interface RegionScheduler {

    /**
     * Runs the given task on the thread that owns the given location.
     * <p>
     * The task is run right away when the calling thread already owns the location, so that a caller which reports
     * what the task did can do so before it returns. Otherwise the task is left to the owning thread and this method
     * returns before the task has run, so anything the task wrote may only be read when {@code true} is returned.
     *
     * @param location the location the task touches
     * @param task     the task to run
     * @return {@code true} if the task has already run when this method returns, or {@code false} if it was left to
     * the thread that owns the location
     */
    boolean execute(@NotNull Location location, @NotNull Runnable task);

}
