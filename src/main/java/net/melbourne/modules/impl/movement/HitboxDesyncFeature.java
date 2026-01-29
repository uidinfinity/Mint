package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.minecraft.util.math.Vec3d;

@FeatureInfo(name = "HitboxDesync", category = Category.Movement)
public class HitboxDesyncFeature extends Feature {

    public BooleanSetting alternating = new BooleanSetting("Alternating", "Modifies your position in such a way that it messes with other clients.", true);
    public BooleanSetting minimal = new BooleanSetting("Minimal", "Makes alternating minimal, only required on certain servers.", true,
            () -> alternating.getValue());
    public BooleanSetting specific = new BooleanSetting("Specific", "Specific alternating mode, only required against certain clients.", true,
            () -> alternating.getValue());
    public final WhitelistSetting toggles = new WhitelistSetting("Toggles", "Module toggle conditions", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Self", "Jump"});

    private long lastTimeReset;
    private double prevY;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            setToggled(false);
            return;
        }

        prevY = mc.player.getY();
        lastTimeReset = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
    }

    @SubscribeEvent(priority = Integer.MAX_VALUE)
    public void onTick(TickEvent event) {
        if (mc.player == null) {
            setToggled(false);
            return;
        }

        if (toggles.getWhitelistIds().contains("Jump") && mc.player.getY() != prevY) {
            setToggled(false);
            return;
        }

        long currentTime = System.currentTimeMillis();
        double timeout = specific.getValue() ? 500 : 1500;

        boolean hasTimeElapsed = (currentTime - lastTimeReset) >= timeout;

        Vec3d vec3d = mc.player.getBlockPos().toCenterPos();
        double offset = minimal.getValue() ? 0.001 : 0.002;

        boolean flag = hasTimeElapsed && alternating.getValue() && !mc.player.isSneaking();
        boolean flagX = (vec3d.x - mc.player.getX()) > 0;
        boolean flagZ = (vec3d.z - mc.player.getZ()) > 0;

        double x = vec3d.x + ((flag ? offset : 0) * (flagX ? 1 : -1)) + 0.20000000009497754 * (flagX ? -1 : 1);
        double z = vec3d.z + ((flag ? offset : 0) * (flagZ ? 1 : -1)) + 0.2000000000949811 * (flagZ ? -1 : 1);

        mc.player.setPosition(x, mc.player.getY(), z);

        if (hasTimeElapsed) {
            lastTimeReset = currentTime;
        }

        if (toggles.getWhitelistIds().contains("Self") && !alternating.getValue()) {
            setToggled(false);
        }
    }
}