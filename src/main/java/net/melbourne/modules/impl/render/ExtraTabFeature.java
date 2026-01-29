package net.melbourne.modules.impl.render;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.NumberSetting;

import java.awt.*;

@FeatureInfo(name = "ExtraTab", category = Category.Render)
public class ExtraTabFeature extends Feature {
    public NumberSetting limit = new NumberSetting("Limit", "The player limit on the tab list.", 1000, 1, 1000);
    public ColorSetting selfColor = new ColorSetting("Self", "The color of your own name.", new Color(255, 255, 255));

    @Override
    public String getInfo() {
        return String.valueOf(limit.getValue().intValue());
    }
}
