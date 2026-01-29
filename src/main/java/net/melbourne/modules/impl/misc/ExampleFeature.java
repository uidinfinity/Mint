package net.melbourne.modules.impl.misc;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.*;
import net.melbourne.utils.graphics.impl.Renderer2D;

import java.awt.*;

// You need to add this annotation to actually add the module to the client.
@FeatureInfo(name = "Example", category = Category.Misc)
public class ExampleFeature extends Feature {

    // Every setting, if structured correctly, will automatically register.
    public BooleanSetting exampleBool = new BooleanSetting("ExampleBoolean", "This is how you create a boolean.", true);

    // The value is "1". The value needs to be present within the modes, in here, the modes are: new String[]{"1", "2", "3"}.
    public ModeSetting exampleMode = new ModeSetting("ExampleMode", "This is how you create a mode/enum setting. We do not use enums.", "1", new String[]{"1", "2", "3"});

    // The NumberSetting utilises the Number class, that means it is not a float, integer, double, etc. You can turn it into your value of choice by appending .floatValue(), .doubleValue(), etc. to achieve your desired number.
    public NumberSetting exampleNumber = new NumberSetting("ExampleNumber", "This is how you create a number setting", 50, 0, 100);

    // You can get the color by doing .getColor(). Not .getValue(). This will get the Color class directly.
    public ColorSetting exampleColor = new ColorSetting("ExampleColor", "This is how you create a color setting", new Color(200, 200, 200));

    // This is how you create a TextSetting, The value, being "TestValue", will give it you, directly as a string. The user can change its value through the UI.
    public TextSetting exampleText = new TextSetting("ExampleText", "This how you create a text setting", "TestValue");
    // This is how you create a WhitelistSetting, you can add or remove items & blocks from & to the whitelist via the UI.
    public WhitelistSetting exampleWhitelistItems = new WhitelistSetting("Items", "This how you create a whitelist setting with the items type.", WhitelistSetting.Type.ITEMS);
    public WhitelistSetting exampleWhitelistCustom = new WhitelistSetting("Custom", "This how you create a whitelist setting with the items type.", WhitelistSetting.Type.CUSTOM, new String[]{"1", "2", "3"});

    // Anytime the module gets enabled, this will be called.
    @Override
    public void onEnable() {
        // getNull() acts as: mc.world == null || mc.player == null.
        if (getNull())
            return;

        // This allows you to control whether you want a feature to be enabled or not. true == it will enable, false == it will disable.
        setEnabled(false);

        // Appending isEnabled() to a class like this, will allow you to see if it is enabled or not as a boolean.
        Managers.FEATURE.getFeatureFromClass(AutoRespawnFeature.class).isEnabled();

        // You can use mc. to get MinecraftClient mc = MinecraftClient.getInstance(); anywhere within a feature. We implement an interface named Globals to achieve this functionality on the Feature class.
        mc.player.getHeight();
    }

    // Anytime the module gets disabled, this will be called.
    @Override
    public void onDisable() {

    }

    // You need to add the SubscribeEvent annotation to every event, or it will not subscribe it to the eventbus.
    @SubscribeEvent
    public void onRenderHud(RenderHudEvent event) {

    }
}
