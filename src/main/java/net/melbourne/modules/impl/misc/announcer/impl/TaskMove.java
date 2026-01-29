package net.melbourne.modules.impl.misc.announcer.impl;


import net.melbourne.modules.impl.misc.announcer.MessagePrefixes;
import net.melbourne.modules.impl.misc.announcer.QueuedTask;
import net.melbourne.modules.impl.misc.announcer.TaskType;
import net.melbourne.utils.Globals;

public class TaskMove extends QueuedTask implements Globals {
    public double posX;
    public double posZ;

    public TaskMove(TaskType type) {
        super(type);
        posX = mc.player.getX();
        posZ = mc.player.getZ();
    }

    public String getMessage() {
        return mc.player.getX() == posX && mc.player.getZ() == posZ
                ? null
                : MessagePrefixes.getMessage(
                TaskType.WALK,
                String.format("%.2f", Math.abs(mc.player.getX() - posX) + Math.abs(mc.player.getZ() - posZ))
        );
    }
}