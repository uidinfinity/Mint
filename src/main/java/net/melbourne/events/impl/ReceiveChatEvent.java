package net.melbourne.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.melbourne.events.Event;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

@Getter
@AllArgsConstructor
public class ReceiveChatEvent extends Event {
    private Text message;
    private MessageIndicator indicator;
    private int id;
    private boolean added = false;

    public void setMessage(Text message) {
        this.message = message;
        this.added = true;
    }

    public void setIndicator(MessageIndicator indicator) {
        this.indicator = indicator;
        this.added = true;
    }

    public boolean hasBeenAdded() {
        return added;
    }
}