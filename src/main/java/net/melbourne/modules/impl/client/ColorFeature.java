package net.melbourne.modules.impl.client;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;

import java.awt.*;

@FeatureInfo(name = "Color", category = Category.Client)
public class ColorFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "Color mode", "Static", new String[]{"Static", "Rainbow"});
    public ColorSetting color = new ColorSetting("Color", "The global color that is used.", new Color(163, 255, 202));
    public NumberSetting rainbowSpeed = new NumberSetting("RainbowSpeed", "Speed of the rainbow", 10, 1, 20);
    public NumberSetting rainbowLength = new NumberSetting("RainbowLength", "Length of the rainbow wave", 8, 1, 20);
    public NumberSetting rainbowSaturation = new NumberSetting("RainbowSaturation", "Saturation of the rainbow", 1.0f, 0.0f, 1.0f);
}
