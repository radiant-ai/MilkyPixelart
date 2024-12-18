package fun.milkyway.milkypixelart.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

//Stores the id of active item frame and task associated with removing it
public class ActiveFrame {
    private final int frameId;
    private final ScheduledTask task;

    public ActiveFrame(int frameId, @NotNull ScheduledTask task) {
        this.frameId = frameId;
        this.task = task;
    }

    public int getFrameId() {
        return frameId;
    }

    public @NotNull ScheduledTask getTask() {
        return task;
    }
}
