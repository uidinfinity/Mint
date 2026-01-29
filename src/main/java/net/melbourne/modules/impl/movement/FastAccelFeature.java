package net.melbourne.modules.impl.movement;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.MoveEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.misc.RoboticsFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.entity.player.movement.MovementUtils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2d;

@FeatureInfo(name = "FastAccel", category = Category.Movement)
public class FastAccelFeature extends Feature {
    public BooleanSetting inAir = new BooleanSetting("InAir", "Air", true);
    public BooleanSetting speedInWater = new BooleanSetting("SpeedInWater", "Water", false);
    public NumberSetting speed = new NumberSetting("Speed", "Speed", 1.0, 0.1, 5.0);

    @SubscribeEvent
    public void onPlayerMove(MoveEvent event) {
        if (getNull() || Managers.FEATURE.getFeatureFromClass(HoleSnapFeature.class).isEnabled() || Managers.FEATURE.getFeatureFromClass(SpeedFeature.class).isEnabled()) return;
        RoboticsFeature r = Managers.FEATURE.getFeatureFromClass(RoboticsFeature.class);
        if (r != null && r.isEnabled() && r.role.getValue().equals("Server")) return;

        if (mc.player.fallDistance >= 5.0f || mc.player.isSneaking() || mc.player.isClimbing() || (mc.player.isInFluid() && !speedInWater.getValue())) return;
        if (!mc.player.isOnGround() && !inAir.getValue()) return;

        Vector2d velocity = MovementUtils.forward(MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED) * speed.getValue().floatValue());
        event.setMovement(new Vec3d(velocity.x, event.getMovement().getY(), velocity.y));
        event.setCancelled(true);
    }
}