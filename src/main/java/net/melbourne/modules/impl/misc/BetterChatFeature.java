package net.melbourne.modules.impl.misc;

import lombok.Getter;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.*;
import net.melbourne.utils.animations.Animation;
import net.minecraft.client.gui.hud.ChatHudLine;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

@FeatureInfo(name = "BetterChat", category = Category.Misc)
public class BetterChatFeature extends Feature {

    public NumberSetting offset = new NumberSetting("Offset", "X-axis offset for chat messages.", 0, -100, 100);
    public NumberSetting textAlpha = new NumberSetting("TextAlpha", "Transparency of the chat text.", 255, 0, 255);

    public ModeSetting animMode = new ModeSetting("Animation", "How messages appear.", "Slide", new String[]{"None", "Slide", "Fade"});
    public NumberSetting delay = new NumberSetting("Delay", "Animation duration in milliseconds.", 200, 50, 2000,
            () -> !animMode.getValue().equals("None"));

    public BooleanSetting timestamps = new BooleanSetting("Timestamps", "Show time before messages.", true);
    public TextSetting opening = new TextSetting("Opening", "Bracket before timestamp.", "<",
            () -> timestamps.getValue());
    public TextSetting closing = new TextSetting("Closing", "Bracket after timestamp.", ">",
            () -> timestamps.getValue());

    public ModeSetting background = new ModeSetting("Background", "Chat background rendering mode.", "Default", new String[]{"Default", "Clear", "Custom"});
    public ColorSetting customColor = new ColorSetting("Color", "Custom background color.", new Color(0, 0, 0, 120),
            () -> background.getValue().equals("Custom"));

    @Getter
    private final Map<ChatHudLine.Visible, Animation> animationMap = new HashMap<>();

    @Override
    public void onDisable() {
        animationMap.clear();
    }
}