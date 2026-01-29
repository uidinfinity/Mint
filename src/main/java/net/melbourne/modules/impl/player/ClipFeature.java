package net.melbourne.modules.impl.player;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.utils.entity.player.PlayerUtils;
import net.minecraft.util.math.MathHelper;

@FeatureInfo(name = "Clip", category = Category.Player)
public class ClipFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "Clip mode.", "Normal", new String[]{"Normal", "CC", "KR"});
    public NumberSetting timeout = new NumberSetting("Timeout", "Delay between nudges.", 5, 1, 10);
    public BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "Disable after clipping.", false);

    private boolean readyToDisable;

    @Override
    public void onEnable() {
        if (getNull()) return;
        readyToDisable = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        if (isPlayerMoving())
            stopPlayerMotion();

        switch (mode.getValue()) {
            case "Normal":
                doClip(false);
                break;

            case "CC":
                doClip(true);
                break;

            case "KR":
                doKRClip();
                break;
        }
    }

    private void doClip(boolean cc) {
        double minX, maxX, minZ, maxZ;

        if (!isInsideBlock()) {
            double magicOffset1 = 0.30000001192093;
            minX = mc.player.getBlockX() + magicOffset1;
            double magicOffset2 = 0.69999998807907;
            maxX = mc.player.getBlockX() + magicOffset2;
            minZ = mc.player.getBlockZ() + magicOffset1;
            maxZ = mc.player.getBlockZ() + magicOffset2;

            PlayerUtils.instantTp(
                    roundToClosest(mc.player.getX(), minX, maxX),
                    mc.player.getY(),
                    roundToClosest(mc.player.getZ(), minZ, maxZ)
            );
            return;
        }

        if (mc.player.age % timeout.getValue().intValue() == 0) {

            double magicOffset = cc ? 1e-9 : 0.03;

            minX = Math.floor(mc.player.getX()) + 0.241;
            maxX = Math.floor(mc.player.getX()) + 0.759;
            minZ = Math.floor(mc.player.getZ()) + 0.241;
            maxZ = Math.floor(mc.player.getZ()) + 0.759;

            double tpX = mc.player.getX() +
                    MathHelper.clamp(
                            roundToClosest(mc.player.getX(), minX, maxX) - mc.player.getX(),
                            -magicOffset,
                            magicOffset
                    );

            double tpZ = mc.player.getZ() +
                    MathHelper.clamp(
                            roundToClosest(mc.player.getZ(), minZ, maxZ) - mc.player.getZ(),
                            -magicOffset,
                            magicOffset
                    );

            PlayerUtils.instantTp(tpX, mc.player.getY(), tpZ);

            if (readyToDisable) {
                setToggled(false);
                return;
            }

            if (autoDisable.getValue()) {
                readyToDisable = !mc.world.isSpaceEmpty(
                        mc.player.getBoundingBox().expand(-magicOffset, 0, -magicOffset)
                );
            }
        }
    }

    private void doKRClip() {
        double forward = 0.0000001;
        double yaw = Math.toRadians(mc.player.getYaw());

        double x = -Math.sin(yaw) * forward;
        double z = Math.cos(yaw) * forward;

        PlayerUtils.instantTp(
                mc.player.getX() + x,
                mc.player.getY(),
                mc.player.getZ() + z
        );
    }

    private boolean isPlayerMoving() {
        return mc.options.forwardKey.isPressed() ||
                mc.options.backKey.isPressed() ||
                mc.options.leftKey.isPressed() ||
                mc.options.rightKey.isPressed() ||
                mc.options.jumpKey.isPressed();
    }

    private void stopPlayerMotion() {
        mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
    }

    private boolean isInsideBlock() {
        return !mc.world.isSpaceEmpty(
                mc.player.getBoundingBox().expand(0.01, 0, 0.01)
        );
    }

    private double roundToClosest(double num, double min, double max) {
        return (max - num > num - min) ? min : max;
    }
}
