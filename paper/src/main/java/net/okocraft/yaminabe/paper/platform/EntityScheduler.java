package net.okocraft.yaminabe.paper.platform;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Runs a task on the thread that owns an entity, which is the only thread that may safely access mutable entity state
 * while the server splits the world into regions, as Folia does.
 */
public interface EntityScheduler {

    /**
     * Runs the given task on the thread that owns the given entity.
     * <p>
     * The task is run right away when the calling thread already owns the entity. Otherwise it is scheduled on the
     * entity's scheduler. A {@code false} result means the entity had already been retired and the task will not run.
     *
     * @param entity the entity the task touches
     * @param task   the task to run
     * @return {@code true} if the task ran or was scheduled, or {@code false} if the entity was already retired
     */
    boolean execute(@NotNull Entity entity, @NotNull Runnable task);

    /**
     * Runs the given task on the thread that owns the given entity and invokes {@code retired} if the entity retires
     * after the task has been accepted but before it can run.
     * <p>
     * Implementations that cannot observe retirement after scheduling may use the default behavior. Callers must still
     * handle a {@code false} return value, which means the entity was already retired when the task was submitted.
     *
     * @param entity  the entity the task touches
     * @param task    the task to run
     * @param retired the callback to run if an accepted task cannot run because the entity retires
     * @return {@code true} if the task ran or was scheduled, or {@code false} if the entity was already retired
     */
    default boolean execute(@NotNull Entity entity, @NotNull Runnable task, @NotNull Runnable retired) {
        return this.execute(entity, task);
    }
}
