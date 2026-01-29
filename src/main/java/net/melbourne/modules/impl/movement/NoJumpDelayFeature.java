package net.melbourne.modules.impl.movement;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;

@FeatureInfo(name = "NoJumpDelay", category = Category.Movement)
public class NoJumpDelayFeature extends Feature {
    public NumberSetting ticks = new NumberSetting("Ticks", "The amount of ticks it takes to start jumping again.", 1, 0, 20);

    @Override
    public String getInfo() {
        return String.valueOf(ticks.getValue());
    }
}
