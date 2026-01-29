package net.melbourne.modules.impl.player;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Items;

@FeatureInfo(name = "NoInteract", category = Category.Player)
public class NoInteractFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "The way that right-clickable blocks will be ignored.", "Sneak", new String[]{"Sneak", "Disable"});
    public BooleanSetting gapple = new BooleanSetting("Gapple", "Only disables interactions when holding a golden apple in your main hand.", false);

    @Override
    public String getInfo() {
        return mode.getValue();
    }

    public boolean shouldNoInteract() {
        return !gapple.getValue() || mc.player.getMainHandStack().getItem().equals(Items.ENCHANTED_GOLDEN_APPLE);
    }
}
