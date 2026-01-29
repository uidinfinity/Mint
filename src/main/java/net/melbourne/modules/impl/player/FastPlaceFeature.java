package net.melbourne.modules.impl.player;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.item.*;

@FeatureInfo(name = "FastPlace", category = Category.Player)
public class FastPlaceFeature extends Feature {
    public ModeSetting items = new ModeSetting("Items", "The categories of items that will be susceptible to FastPlace", "All", new String[]{"All", "Blocks", "Projectiles"});
    public NumberSetting delay = new NumberSetting("Delay", "The delay between each action", 0, 0, 4);

    public boolean allowItem(Item item) {
        if ((items.getValue().equalsIgnoreCase("Projectiles") || items.getValue().equalsIgnoreCase("All")) &&
                (item instanceof EggItem ||
                        item instanceof ExperienceBottleItem ||
                        item instanceof FireChargeItem ||
                        item instanceof SnowballItem ||
                        item instanceof ThrowablePotionItem ||
                        item instanceof WindChargeItem))
            return true;

        return (items.getValue().equalsIgnoreCase("Blocks") || items.getValue().equalsIgnoreCase("All")) && item instanceof BlockItem;
    }

    @Override
    public String getInfo() {
        return delay.getValue().intValue() + "";
    }
}
