package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.ducks.ISimpleOption;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@FeatureInfo(name = "Fullbright", category = Category.Render)
public class FullbrightFeature extends Feature {
    public ModeSetting mode = new ModeSetting("Mode", "The method to light up the game.", "Gamma", new String[]{"Gamma", "Potion"});

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mode.getValue().equalsIgnoreCase("Potion")) {
            if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE));
        } else {
            if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);

            ((ISimpleOption) (Object) mc.options.getGamma()).melbourne$setValue(10000.0);
        }
    }

    @Override
    public void onEnable() {
        if (getNull())
            return;

        if (!mode.getValue().equalsIgnoreCase("Potion"))
            return;

        if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE));
    }

    @Override
    public void onDisable() {
        if (getNull())
            return;

        if (!mode.getValue().equalsIgnoreCase("Potion"))
            return;

        if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);

        if (mc.options != null)
            ((ISimpleOption) (Object) mc.options.getGamma()).melbourne$setValue(1.0);
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}
