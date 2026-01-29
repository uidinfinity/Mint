package net.melbourne.modules.impl.movement;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@FeatureInfo(name = "AntiVoid", category = Category.Movement)
public class AntiVoidFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "Anti-void mode.", "Fling", new String[]{"Fling", "Float", "Bounce"});

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;

        BlockPos playerPos = mc.player.getBlockPos();

        BlockPos bottomPos = new BlockPos(mc.player.getBlockX(), mc.world.getBottomY(), mc.player.getBlockZ());

        if (mc.world.getBlockState(bottomPos).isOf(Blocks.AIR)) {

            if (playerPos.getY() <= mc.world.getBottomY()) {

                switch (mode.getValue()) {
                    case "Fling":
                        mc.player.setVelocity(mc.player.getVelocity().withAxis(Direction.Axis.Y, 1.0));
                        break;
                    case "Bounce":
                        mc.player.jump();
                        break;
                    case "Float":
                        mc.player.setVelocity(mc.player.getVelocity().withAxis(Direction.Axis.Y, 0.0));
                        break;
                }
            }
        }
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}