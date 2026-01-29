package net.melbourne.modules.impl.misc.announcer.impl;

import net.melbourne.modules.impl.misc.announcer.QueuedTask;
import net.melbourne.modules.impl.misc.announcer.TaskType;

public class TaskBasic extends QueuedTask {
    public String message;

    public TaskBasic(TaskType type, String message) {
        super(type);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}