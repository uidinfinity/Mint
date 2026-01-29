package net.melbourne.modules.impl.misc;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.TextSetting;

@FeatureInfo(name = "NameProtect", category = Category.Misc)
public class NameProtectFeature extends Feature {

    public static NameProtectFeature INSTANCE;

    public final TextSetting replacement = new TextSetting("Replacement", "Text to replace your name with", "REDACTED");

    public NameProtectFeature() {
        INSTANCE = this;
    }
}