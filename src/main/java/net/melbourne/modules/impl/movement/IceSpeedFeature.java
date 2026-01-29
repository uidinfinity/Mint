package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.mixins.accessors.AbstractBlockAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.miscellaneous.math.MathUtils;
import net.minecraft.block.Blocks;

@FeatureInfo(name = "IceSpeed", category = Category.Movement)
public class IceSpeedFeature extends Feature {
    public NumberSetting slipperiness = new NumberSetting("Slipperiness", "How much the ice will affect your movement.", 0.4, 0.2, 1.5);

    @Override
    public void onDisable() {
        if (getNull())
            return;

        ((AbstractBlockAccessor) Blocks.ICE).setSlipperiness(0.98f);
        ((AbstractBlockAccessor) Blocks.PACKED_ICE).setSlipperiness(0.98f);
        ((AbstractBlockAccessor) Blocks.FROSTED_ICE).setSlipperiness(0.98f);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull())
            return;

        ((AbstractBlockAccessor) Blocks.ICE).setSlipperiness(slipperiness.getValue().floatValue());
        ((AbstractBlockAccessor) Blocks.PACKED_ICE).setSlipperiness(slipperiness.getValue().floatValue());
        ((AbstractBlockAccessor) Blocks.FROSTED_ICE).setSlipperiness(slipperiness.getValue().floatValue());
    }

    @Override
    public String getInfo() {
        return MathUtils.round(slipperiness.getValue().doubleValue(), 1) + "";
    }
}
