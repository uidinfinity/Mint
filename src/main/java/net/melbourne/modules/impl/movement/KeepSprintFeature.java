package net.melbourne.modules.impl.movement;


import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;

@FeatureInfo(name = "KeepSprint", category = Category.Movement)
public class KeepSprintFeature extends Feature {

    public NumberSetting motion = new NumberSetting("Motion", "The velocity that will be applied to your movement when attacking.", 100.0f, 0.0f, 100.0f);

    @Override
    public String getInfo() {
        return String.format("%.1f", motion.getValue().doubleValue()) + "%";
    }
}
