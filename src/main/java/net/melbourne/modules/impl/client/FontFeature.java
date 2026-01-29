package net.melbourne.modules.impl.client;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.TextSetting;
import net.melbourne.utils.font.FontRenderer;
import net.melbourne.utils.graphics.impl.font.FontUtils;

import java.awt.*;

@FeatureInfo(name = "Font", category = Category.Client)
public class FontFeature extends Feature {
    public TextSetting name = new TextSetting("Name", "The name of the font that will be rendered.", "Tahoma");
    public NumberSetting size = new NumberSetting("Size", "The size of the custom font that will be rendered.", 18, 8, 48);
    public ModeSetting style = new ModeSetting("Style", "The style that will be used in the font's rendering.", "Plain", new String[]{"Plain", "Bold", "Italic", "BoldItalic"});
    public NumberSetting xOffset = new NumberSetting("X-Offset", "The offset that will be applied to the font on the X axis.", 0, -10, 10);
    public NumberSetting yOffset = new NumberSetting("Y-Offset", "The offset that will be applied to the font on the Y axis.", 0, -10, 10);
    public NumberSetting shadowOffset = new NumberSetting("Shadow-Offset", "The distance of the shadow from the text being rendered.", 0.5f, -2.0f, 2.0f);
    public BooleanSetting global = new BooleanSetting("Global", "Override all Minecraft text with custom font.", false);

    @SubscribeEvent
    public void onSettingChange(SettingChangeEvent event) {
        if (getNull()) return;
        if (event.getSetting() == name || event.getSetting() == size || event.getSetting() == style) {
            updateFontRenderer();
        }
    }

    @Override
    public void onEnable() {
        updateFontRenderer();
    }

    private void updateFontRenderer() {
        FontUtils.fontRenderer = (new FontRenderer(new Font[]{new Font(name.getValue(), style.getValue().equalsIgnoreCase("BoldItalic") ? Font.BOLD | Font.ITALIC : style.getValue().equalsIgnoreCase("Bold") ? Font.BOLD : style.getValue().equalsIgnoreCase("Italic") ? Font.ITALIC : Font.PLAIN, size.getValue().intValue())}, size.getValue().floatValue() / 2.0f));
    }
}