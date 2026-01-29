package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.*;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.graphics.api.WorldContext;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.awt.*;

@FeatureInfo(name = "Trajectories", category = Category.Render)
public class TrajectoriesFeature extends Feature {

    public final ColorSetting fill = new ColorSetting("Fill", "Trajectory fill color", new Color(255, 255, 255, 70));
    public final ColorSetting outline = new ColorSetting("Outline", "Trajectory outline color", new Color(255, 255, 255));

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent e) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden || !mc.options.getPerspective().isFirstPerson())
            return;

        Item item = getHeldItem();
        if (item == null)
            return;

        boolean prevBob = mc.options.getBobView().getValue();
        mc.options.getBobView().setValue(false);

        float yaw = mc.player.getYaw();

        if (isMultishot(item)) for (float offset : new float[]{-10, 0, 10}) project(e.getContext(), item, yaw + offset, e.getTickDelta());
        else project(e.getContext(), item, yaw, e.getTickDelta());

//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        mc.options.getBobView().setValue(prevBob);
    }

    private Item getHeldItem() {
        Item main = mc.player.getMainHandStack().getItem();
        Item off = mc.player.getOffHandStack().getItem();
        if (isThrowable(main)) return main;
        if (isThrowable(off)) return off;
        return null;
    }

    private boolean isThrowable(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || EntityUtils.isThrowable(item);
    }

    private boolean isMultishot(Item item) {
        return (item instanceof CrossbowItem) &&
                EnchantmentHelper.getLevel(
                        mc.world.getRegistryManager()
                                .getOrThrow(RegistryKeys.ENCHANTMENT)
                                .getOrThrow(Enchantments.MULTISHOT),
                        mc.player.getStackInHand(mc.player.getActiveHand())
                ) > 0;
    }

    private void project(WorldContext ctx, Item item, float yaw, float tickDelta) {
        double rad = Math.toRadians(yaw);
        Vec3d pos = mc.player.getCameraPosVec(tickDelta).add(-Math.cos(rad) * 0.16, -0.1, -Math.sin(rad) * 0.16);
        Vec3d vel = getVelocity(item, yaw);

        if (!mc.player.isOnGround()) vel = vel.add(0, mc.player.getVelocity().y, 0);
        simulate(ctx, pos, vel, item);
    }

    private Vec3d getVelocity(Item item, float yaw) {
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(mc.player.getPitch() - getThrowPitch(item));
        float power = MathHelper.clamp(mc.player.getItemUseTime() / 20f, 0, 1);
        float mul = (item instanceof BowItem ? power * 2 : item instanceof CrossbowItem ? 2.2f : 1f) * getThrowVelocity(item);

        double x = -Math.sin(radYaw) * Math.cos(radPitch) * mul;
        double y = -Math.sin(radPitch) * mul;
        double z = Math.cos(radYaw) * Math.cos(radPitch) * mul;
        return new Vec3d(x, y, z);
    }

    private void simulate(WorldContext ctx, Vec3d pos, Vec3d vel, Item item) {
        for (int i = 0; i < 300 && pos.y > -64; i++) {
            Vec3d next = pos.add(vel);
            boolean water = mc.world.getBlockState(BlockPos.ofFloored(next)).isOf(Blocks.WATER);
            double friction = water ? 0.8 : 0.99;
            vel = vel.multiply(friction, friction, friction)
                    .add(0, -(item instanceof BowItem || item instanceof CrossbowItem ? 0.05 : 0.03), 0);

            HitResult hit = mc.world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            Renderer3D.renderLine(ctx, pos, next, outline.getColor(), outline.getColor());

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                Vec3d h = hit.getPos();
                Box box = new Box(h.x - 0.3, h.y - 0.01, h.z - 0.3, h.x + 0.3, h.y + 0.01, h.z + 0.3);
                Renderer3D.renderBox(ctx, box, fill.getColor());
                Renderer3D.renderBoxOutline(ctx, box, outline.getColor());
                break;
            }
            pos = next;
        }
    }

    private float getThrowVelocity(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return 0.5f;
        if (item instanceof ExperienceBottleItem) return 0.59f;
        if (item instanceof TridentItem) return 2f;
        return 1.5f;
    }

    private int getThrowPitch(Item item) {
        return (item instanceof ThrowablePotionItem || item instanceof ExperienceBottleItem) ? 20 : 0;
    }
}

