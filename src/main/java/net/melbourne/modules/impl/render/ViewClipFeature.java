package net.melbourne.modules.impl.render;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;

@FeatureInfo(name = "ViewClip", category = Category.Render)
public class ViewClipFeature extends Feature {
    public NumberSetting distance = new NumberSetting("Distance", "Changes the reach you have with viewclip", 4.0f, -50.0f, 50.0f);
}
