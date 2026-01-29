package net.melbourne.modules.impl.player;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.minecraft.client.option.KeyBinding;

@FeatureInfo(name = "AutoWalk", category = Category.Player)
public class AutoWalkFeature extends Feature {

    @Override
    public void onDisable() {
        if (getNull())
            return;

        KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), false);
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull())
            return;

        KeyBinding.setKeyPressed(mc.options.forwardKey.getDefaultKey(), true);
    }
}
