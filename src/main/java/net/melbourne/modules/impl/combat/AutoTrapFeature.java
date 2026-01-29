package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

@FeatureInfo(name = "AutoTrap", category = Category.Combat)
public class AutoTrapFeature extends PlaceFeature {

    public final ModeSetting trapMode = new ModeSetting("Mode", "Placement layout", "Full", new String[]{"Partial", "Full"});
    public final BooleanSetting head = new BooleanSetting("Head", "Cover head", true, () -> trapMode.getValue().equalsIgnoreCase("Full"));
    public final BooleanSetting antiStep = new BooleanSetting("AntiStep", "Prevent step out", false);
    public final BooleanSetting holeCheck = new BooleanSetting("HoleCheck", "Only trap if in hole", true);
    public final BooleanSetting cev = new BooleanSetting("CEV", "Do not place head block until the crystal is broken.", false);

    public final BooleanSetting cevRebreak = new BooleanSetting("CEVRebreak", "Keep trying to break the CEV crystal until it is gone.", true, cev::getValue);
    public final BooleanSetting cevInstant = new BooleanSetting("CEVInstant", "Wait then allow head place if crystal is gone.", true, cev::getValue);
    public final NumberSetting cevInstantDelay = new NumberSetting("CEVInstantDelay", "Delay before allowing head place (ms).", 100, 0, 500, () -> cev.getValue() && cevInstant.getValue());
    public final NumberSetting cevRebreakDelay = new NumberSetting("CEVRebreakDelay", "Delay between break attempts (ms).", 75, 0, 500, () -> cev.getValue() && cevRebreak.getValue());

    private final Timer timer = new Timer();
    private final Timer gateTimer = new Timer();
    private BlockPos cevPos = null;
    private boolean active = false;

    @Override
    public void onEnable() {
        super.onEnable();
        active = false;
        cevPos = null;
        timer.reset();
        gateTimer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        active = false;
        cevPos = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || shouldDisable()) return;

        PlayerEntity target = findTarget();
        if (target == null) {
            active = false;
            cevPos = null;
            return;
        }
        if (holeCheck.getValue() && !HoleUtils.isPlayerInHole(target)) {
            active = false;
            cevPos = null;
            return;
        }

        List<BlockPos> trapPositions = HoleUtils.getTrapPositions(
                target,
                trapMode.getValue().equalsIgnoreCase("Partial"),
                head.getValue(),
                antiStep.getValue(),
                false,
                strictDirection.getValue()
        );

        if (active && (cevPos == null || !trapPositions.contains(cevPos))) {
            active = false;
            cevPos = null;
        }

        Slot hotbarBlock = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.OBSIDIAN);
        if (hotbarBlock == null) hotbarBlock = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.ENDER_CHEST);
        if (hotbarBlock == null) return;

        List<BlockPos> valid = new ArrayList<>();

        for (BlockPos pos : trapPositions) {
            if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > placeRange.getValue().doubleValue()) continue;

            if (cev.getValue() && isHeadCapPos(pos, target)) {
                if (!hasCrystalAt(pos)) {
                    if (active && pos.equals(cevPos)) {
                        active = false;
                        cevPos = null;
                    }
                    if (canPlace(pos)) valid.add(pos);
                    continue;
                }

                if (!active || cevPos == null || !cevPos.equals(pos)) {
                    active = true;
                    cevPos = pos;
                    gateTimer.reset();
                    timer.reset();
                }

                if (cevRebreak.getValue()) {
                    if (timer.hasTimeElapsed(cevRebreakDelay.getValue().longValue())) {
                        breakCevCrystal(pos);
                        timer.reset();
                    }
                } else {
                    breakCevCrystal(pos);
                }

                if (cevInstant.getValue()) {
                    if (gateTimer.hasTimeElapsed(cevInstantDelay.getValue().longValue())) {
                        if (!hasCrystalAt(pos)) {
                            active = false;
                            cevPos = null;
                            if (canPlace(pos)) valid.add(pos);
                        }
                    }
                }

                continue;
            }

            if (canPlace(pos)) valid.add(pos);
        }

        if (valid.isEmpty()) return;

        placeBlocks(valid, Items.OBSIDIAN, Items.ENDER_CHEST);
    }

    private void breakCevCrystal(BlockPos pos) {
        AutoCrystalFeature ca = Managers.FEATURE.getFeatureFromClass(AutoCrystalFeature.class);
        if (ca != null) ca.doBreakFromOtherModule(pos);
    }

    private boolean isHeadCapPos(BlockPos pos, PlayerEntity target) {
        int baseY = (int) Math.floor(target.getY());
        return pos.getY() == baseY + 2;
    }

    private boolean hasCrystalAt(BlockPos pos) {
        if (mc.world == null) return false;
        Box box = new Box(pos).expand(0.6);
        return !mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true).isEmpty();
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && !Managers.FRIEND.isFriend(p.getName().getString()))
                .filter(p -> mc.player.distanceTo(p) <= 8.0f)
                .min((a, b) -> Double.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)))
                .orElse(null);
    }
}
