package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.utils.entity.player.PlayerUtils;
import net.minecraft.entity.effect.StatusEffects;

@FeatureInfo(name = "Sprint", category = Category.Movement)
public class SprintFeature extends Feature {
    public ModeSetting mode = new ModeSetting("Mode", "Allows you to switch the circumstances in which you will sprint.", "Rage", new String[]{"Legit", "Rage"});

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull())
            return;

        if (mode.getValue().equals("Rage")) {
            if (canSprint())
                mc.player.setSprinting(true);
        }

        if (mode.getValue().equals("Legit")) {
            mc.options.sprintKey.setPressed(true);
        }
    }

    public boolean canSprint() {
        return mc.player != null &&
                !mc.player.isSneaking() &&
                !mc.player.horizontalCollision &&
                PlayerUtils.isMoving() &&
                (mc.player.getHungerManager().getFoodLevel() > 6.0f ||
                        mc.player.isGliding()) && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}