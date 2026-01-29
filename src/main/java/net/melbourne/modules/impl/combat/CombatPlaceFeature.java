package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.entity.EntityUtils;
import net.minecraft.entity.player.PlayerEntity;
// reverted to the standard class for 1.21
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FeatureInfo(name = "CombatPlace", category = Category.Combat)
public class CombatPlaceFeature extends PlaceFeature {

    public final BooleanSetting flatten = new BooleanSetting("Flatten", "Place blocks under the target", true);
    public final ModeSetting mode = new ModeSetting("Mode", "Prediction mode", "Two", new String[]{"None", "One", "Two", "Three"});
    public final ModeSetting targetPriority = new ModeSetting("Priority", "Target selection priority", "Closest", new String[]{"Closest", "Health"});
    public final NumberSetting predictTicks = new NumberSetting("PredictTicks", "Prediction scale", 2, 1, 8);
    public final NumberSetting minKmh = new NumberSetting("MinKMH", "Min speed to trigger blocker", 20, 1, 40);
    public final BooleanSetting strict = new BooleanSetting("Strict", "Prevent placement if entities intersect", true);
    public final BooleanSetting safety = new BooleanSetting("Safety", "Prevent trapping yourself", true);

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull() || shouldDisable()) return;
        PlayerEntity target = findTarget();
        if (target != null) doPlace(target);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull() || shouldDisable()) return;

        if (event.getPacket() instanceof EntityS2CPacket packet) {
            PlayerEntity currentTarget = findTarget();

            if (currentTarget == null || packet.getEntity(mc.world) != currentTarget) return;
            doPlace(currentTarget);
        }

    }

    private void doPlace(PlayerEntity target) {
        List<BlockPos> blocksToPlace = getPlacePositions(target);
        if (blocksToPlace.isEmpty()) return;

        double rangeSq = Math.pow(placeRange.getValue().doubleValue(), 2);
        blocksToPlace.removeIf(pos -> mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > rangeSq);

        if (blocksToPlace.isEmpty()) return;

        placeBlocks(blocksToPlace);
    }

    private List<BlockPos> getPlacePositions(PlayerEntity target) {
        List<BlockPos> positions = new ArrayList<>();

        if (flatten.getValue()) {
            BlockPos under = BlockPos.ofFloored(target.getPos()).down();
            if (isValidSpot(under)) positions.add(under);
        }

        String currentMode = mode.getValue();
        if (!currentMode.equals("None")) {
            double speedKmh = EntityUtils.getSpeed(target, EntityUtils.SpeedUnit.KILOMETERS) * 72;

            if (speedKmh >= minKmh.getValue().doubleValue()) {
                Vec3d velocity = target.getVelocity();
                double scale = predictTicks.getValue().doubleValue();
                BlockPos feetPos = BlockPos.ofFloored(target.getX() + velocity.x * scale, target.getY(), target.getZ() + velocity.z * scale);

                if (isValidSpot(feetPos)) {
                    positions.add(feetPos);

                    if (currentMode.equals("Two") || currentMode.equals("Three")) {
                        BlockPos headPos = feetPos.up();
                        if (isValidSpot(headPos)) positions.add(headPos);
                    }

                    if (currentMode.equals("Three")) {
                        BlockPos topPos = feetPos.up(2);
                        if (isValidSpot(topPos)) positions.add(topPos);
                    }
                }
            }
        }
        return positions;
    }

    private boolean isValidSpot(BlockPos pos) {
        if (mc.world.isOutOfHeightLimit(pos)) return false;
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        if (strict.getValue()) {
            if (!mc.world.canPlace(mc.world.getBlockState(pos), pos, null)) return false;
        }

        if (safety.getValue()) {
            if (mc.player.getBoundingBox().intersects(new Box(pos))) return false;
        }

        return true;
    }

    private PlayerEntity findTarget() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && !p.isDead() && !Managers.FRIEND.isFriend(p.getName().getString()))
                .filter(p -> mc.player.distanceTo(p) <= placeRange.getValue().doubleValue() + 4.0)
                .min(getComparator())
                .orElse(null);
    }

    private Comparator<PlayerEntity> getComparator() {
        if (targetPriority.getValue().equals("Health")) {
            return Comparator.comparingDouble(p -> p.getHealth() + p.getAbsorptionAmount());
        }
        return Comparator.comparingDouble(p -> mc.player.distanceTo(p));
    }
}