package fun.milkyway.milkypixelart.utils;

import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

//Stores the id of active item frame and task associated with removing it
public class ActiveFrame {
    private final int frameId;
    private final BukkitTask task;

    public ActiveFrame(int frameId, @NotNull BukkitTask task) {
        this.frameId = frameId;
        this.task = task;
    }

    public int getFrameId() {
        return frameId;
    }

    public @NotNull BukkitTask getTask() {
        return task;
    }
}
