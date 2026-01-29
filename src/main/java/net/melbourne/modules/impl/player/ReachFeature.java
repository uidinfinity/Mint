package net.melbourne.modules.impl.player;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;


@FeatureInfo(name = "Reach", category = Category.Player)
public class ReachFeature extends Feature {
    public NumberSetting amount = new NumberSetting("Amount", "The maximum distance at which you will be able to interact with blocks.", 6.0, 0.0, 8.0);

    @Override
    public String getInfo() {
        return String.valueOf(amount.getValue().doubleValue());
    }
}
