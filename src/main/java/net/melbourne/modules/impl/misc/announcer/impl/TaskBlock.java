package net.melbourne.modules.impl.misc.announcer.impl;

import net.melbourne.modules.impl.misc.announcer.MessagePrefixes;
import net.melbourne.modules.impl.misc.announcer.QueuedTask;
import net.melbourne.modules.impl.misc.announcer.TaskType;
import net.minecraft.block.Block;
import net.minecraft.text.Text;

public class TaskBlock extends QueuedTask {
    public Block block;
    public int count = 1;

    public TaskBlock(TaskType type, Block block) {
        super(type);
        this.block = block;
    }

    public String getMessage() {
        return MessagePrefixes.getMessage(type, Text.translatable(block.getTranslationKey()).getString(), String.valueOf(count));
    }
}