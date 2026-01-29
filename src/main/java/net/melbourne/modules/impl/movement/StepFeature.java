package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@FeatureInfo(name = "Step", category = Category.Movement)
public class StepFeature extends Feature {
    public final ModeSetting mode = new ModeSetting("Mode", "Step mode", "Vanilla", new String[]{"Vanilla", "Normal"});
    public final NumberSetting height = new NumberSetting("Height", "Step Height", 1.0f, 0.0f, 2.5f);

    private double lastY = 0.0;

    public float getHeight() {
        return height.getValue().floatValue();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        double currentY = mc.player.getY();
        double stepHeight = currentY - lastY;

        if (mode.getValue().equals("Normal") && stepHeight > 0.5 && stepHeight <= height.getValue().floatValue()) {
            double[] offsets = getOffset(stepHeight);
            if (offsets != null) {
                for (double offset : offsets)
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + offset, mc.player.getZ(), false, false));
            }
        }

        lastY = currentY;
    }

    public double[] getOffset(double height) {
        if (Math.abs(height - 0.75) < 0.01) {
            return new double[]{0.42, 0.753, 1};
        } else if (Math.abs(height - 0.8125) < 0.01) {
            return new double[]{0.39, 0.7, 0.8125};
        } else if (Math.abs(height - 0.875) < 0.01) {
            return new double[]{0.39, 0.7, 0.875};
        } else if (Math.abs(height - 1.0) < 0.01) {
            return new double[]{0.42, 0.753, 1.0};
        } else if (Math.abs(height - 1.5) < 0.01) {
            return new double[]{0.42, 0.75, 1.0, 1.16, 1.23, 1.2};
        } else if (Math.abs(height - 2.0) < 0.01) {
            return new double[]{0.42, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43};
        } else if (Math.abs(height - 2.5) < 0.01) {
            return new double[]{0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907};
        }
        return null;
    }

    @Override
    public String getInfo() {
        return mode.getValue() + ", " + height.getValue().floatValue();
    }
}