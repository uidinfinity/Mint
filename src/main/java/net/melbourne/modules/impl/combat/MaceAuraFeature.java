package net.melbourne.modules.impl.combat;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@FeatureInfo(name = "MaceAura", category = Category.Combat)
public class MaceAuraFeature extends Feature {

    public final ModeSetting targetMode = new ModeSetting("TargetMode", "Targeting priority", "Single", new String[]{"Single", "Switch"});
    public final ModeSetting swapMode = new ModeSetting("MaceMode", "Mace handling", "Switch", new String[]{"None", "Switch", "Require"});
    public final NumberSetting range = new NumberSetting("Range", "Attack range when visible", 5.0f, 1.0f, 6.0f);
    public final NumberSetting wallRange = new NumberSetting("WallRange", "Attack range through walls", 3.5f, 1.0f, 6.0f);
    public final BooleanSetting invis = new BooleanSetting("Invisibles", "Target invisible entities", true);
    public final BooleanSetting filled = new BooleanSetting("Filled", "Whether to fill the highlight box", true);

    private long lastAttackTime = 0L;
    private final long attackCooldownMillis = 600L;
    private final long hitFadeMillis = 550L;
    private Entity target = null;
    private long hitTime = 0L;

    @Override
    public void onEnable() {
        target = null;
        hitTime = 0L;
    }

    @Override
    public void onDisable() {
        target = null;
        hitTime = 0L;
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (getNull()) { target = null; return; }
        if (!handleMaceSwap()) return;
        target = findTarget();
        if (target == null) return;

        long now = System.currentTimeMillis();
        if (now - lastAttackTime < attackCooldownMillis) return;
        if (mc.player.getAttackCooldownProgress(0) < 1f) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = now;
        hitTime = now;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent e) {
        if (target == null || getNull())
            return;

        long now = System.currentTimeMillis();
        long elapsed = now - hitTime;
        if (elapsed >= hitFadeMillis)
            return;

        double alpha = 1.0 - (double) elapsed / hitFadeMillis;
        int aOutline = (int) (255 * alpha);
        int aFilled  = (int) (70 * alpha);

        double distSq = mc.player.squaredDistanceTo(target);
        double maxRange = canSee(target) ? range.getValue().doubleValue() : wallRange.getValue().doubleValue();
        if (distSq > maxRange * maxRange) return;

        double interpX = target.lastRenderX + (target.getX() - target.lastRenderX) * e.getTickDelta();
        double interpY = target.lastRenderY + (target.getY() - target.lastRenderY) * e.getTickDelta();
        double interpZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * e.getTickDelta();
        Box box = target.getBoundingBox().offset(interpX - target.getX(), interpY - target.getY(), interpZ - target.getZ());

        if (filled.getValue()) Renderer3D.renderBox(e.getContext(), box, ColorUtils.getGlobalColor(aFilled));
        Renderer3D.renderBoxOutline(e.getContext(), box, ColorUtils.getGlobalColor(aOutline));
    }

    private boolean handleMaceSwap() {
        String mode = swapMode.getValue();
        if (mode.equals("None")) return true;

        int maceSlot = findMaceSlot();
        if (maceSlot == -1) return mode.equals("Switch");

        if (mode.equals("Require") && mc.player.getInventory().getSelectedSlot() != maceSlot) return false;
        if (mode.equals("Switch") && mc.player.getInventory().getSelectedSlot() != maceSlot) {
            mc.player.getInventory().setSelectedSlot(maceSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
        }
        return true;
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private Entity findTarget() {
        List<Entity> valid = mc.world.getEntitiesByClass(Entity.class,
                        mc.player.getBoundingBox().expand(range.getValue().doubleValue()),
                        entity -> entity != mc.player && entity.isAlive() && !entity.isRemoved()
                                && (invis.getValue() || !entity.isInvisible())
                                && entity instanceof PlayerEntity)
                .stream()
                .filter(e -> {
                    double d2 = mc.player.squaredDistanceTo(e);
                    double r2 = range.getValue().doubleValue() * range.getValue().doubleValue();
                    double wr2 = wallRange.getValue().doubleValue() * wallRange.getValue().doubleValue();
                    return d2 <= r2 && (canSee(e) || d2 <= wr2);
                })
                .collect(Collectors.toList());

        if (valid.isEmpty()) return null;

        String mode = targetMode.getValue();
        if (mode.equals("Single")) {
            return valid.stream()
                    .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
                    .orElse(null);
        } else { // Switch
            return valid.stream()
                    .filter(e -> e instanceof LivingEntity)
                    .map(e -> (LivingEntity) e)
                    .min(Comparator.comparingDouble(this::calculatePriorityScore))
                    .map(e -> (Entity) e)
                    .orElse(valid.get(0));
        }
    }

    private double calculatePriorityScore(LivingEntity entity) {
        double distance = mc.player.distanceTo(entity);
        double health   = entity.getHealth();
        double armor    = entity.getArmor();
        return (health * 0.4) + (distance * 0.5) - (armor * 0.1);
    }


    private boolean canSeePoint(Vec3d point) {
        HitResult hit = mc.world.raycast(new RaycastContext(
                mc.player.getCameraPosVec(1f), point,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private boolean canSee(Entity e) {
        Vec3d eye = mc.player.getCameraPosVec(1f);
        Vec3d tgt = e.getLerpedPos(1f).add(0, e.getHeight() * 0.5, 0);
        HitResult hit = mc.world.raycast(new RaycastContext(
                eye, tgt, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS ||
                (hit.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hit).getEntity() == e);
    }

    @Override
    public String getInfo() {
        return targetMode.getValue();
    }
}