package net.melbourne.services.impl;

import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.services.Service;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.KeyboardEvent;
import net.melbourne.events.impl.MouseEvent;
import net.melbourne.modules.Feature;

public class BindService extends Service {

    public BindService() {
        super("Bind", "Handles key input functions within the client");
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onKeyboard(KeyboardEvent event) {
        for (Feature feature : Managers.FEATURE.getFeatures()) {
            if (feature.getBind().getValue() == event.getKey()) {
                if (event.isAction()) {
                    switch (feature.getBindMode().getValue()) {
                        case "Toggle":
                            feature.setEnabled(!feature.isEnabled());
                            break;
                        case "Hold":
                            feature.setEnabled(true);
                            break;
                    }
                } else {
                    if (feature.getBindMode().getValue().equals("Hold"))
                        feature.setEnabled(false);
                }
            }

//          if (!BotManager.INSTANCE.isAuthed())
//              System.exit(0);
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        for (Feature feature : Managers.FEATURE.getFeatures()) {
            if (feature.getBind().getValue() == (-event.getButton() - 1)) {
                feature.setEnabled(!feature.isEnabled());
            }
        }
    }
}
