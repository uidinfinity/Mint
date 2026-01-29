package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.util.math.Vec3d;

@FeatureInfo(name = "FastFall", category = Category.Movement)
public class FastFallFeature extends Feature {
    public NumberSetting speed = new NumberSetting("Speed", "Speed at which you fall at.", 1.0, 0.0, 10.0);

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull())
            return;

        if (mc.options.jumpKey.isPressed())
            return;

        if (mc.player.isInLava() || mc.player.isTouchingWater())
            return;

        if (mc.player.isOnGround()) {
            Vec3d currentVel = mc.player.getVelocity();
            mc.player.setVelocity(currentVel.x, -speed.getValue().doubleValue(), currentVel.z);
        }
    }
}