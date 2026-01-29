package net.melbourne.modules.impl.render;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;

@FeatureInfo(name = "HandProgress", category = Category.Render)
public class HandProgressFeature extends Feature {

    public final BooleanSetting modifyMainHand = new BooleanSetting("ModifyMainHand", "Allows changing the main hand position", true);
    public final NumberSetting mainHandProgress = new NumberSetting("MainHandProgress", "The vertical offset of the main hand", 1.0, -1.0, 1.0,
            modifyMainHand::getValue);

    public final BooleanSetting modifyOffHand = new BooleanSetting("ModifyOffHand", "Allows changing the off hand position", true);
    public final NumberSetting offHandProgress = new NumberSetting("OffHandProgress", "The vertical offset of the off hand", 1.0, -1.0, 1.0,
            modifyOffHand::getValue);

    public final BooleanSetting staticEating = new BooleanSetting("StaticConsume", "Freezes eating/drinking animation", false);
}