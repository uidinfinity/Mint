package net.melbourne.modules.impl.render;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.NumberSetting;

import java.awt.*;

@FeatureInfo(name = "Atmosphere", category = Category.Render)
public class AtmosphereFeature extends Feature {

    public BooleanSetting customSky = new BooleanSetting("CustomSky", "Enable custom sky coloring.", false);
    public ColorSetting skyColor = new ColorSetting("SkyColor", "The color of the sky.", new Color(255, 255, 255), 
            () -> customSky.getValue());

    public BooleanSetting customAmbience = new BooleanSetting("Ambience", "Enable custom world ambience.", false);
    public ColorSetting ambienceColor = new ColorSetting("AmbienceColor", "The color of the world lighting.", new Color(255, 255, 255), 
            () -> customAmbience.getValue());
    public NumberSetting vibrancy = new NumberSetting("Vibrancy", "World vibrancy level.", 0.35f, 0.05f, 1.0f, 
            () -> customAmbience.getValue());

    public BooleanSetting timeChanger = new BooleanSetting("TimeChanger", "Change the world time.", false);
    public NumberSetting time = new NumberSetting("Time", "The world time.", 18000, 0, 24000, 
            () -> timeChanger.getValue());

    public BooleanSetting fogModifier = new BooleanSetting("FogModifier", "Modify world fog.", false);
    public ColorSetting fogColor = new ColorSetting("FogColor", "The color of the fog.", new Color(255, 255, 255), 
            () -> fogModifier.getValue());
    public NumberSetting fogDistance = new NumberSetting("FogDistance", "Fog start distance.", 0.5f, 0.1f, 1.0f, 
            () -> fogModifier.getValue());

}