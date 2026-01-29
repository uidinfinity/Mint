package net.melbourne.modules.impl.combat;

import net.melbourne.Melbourne;
import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.entity.CrystalUtils;
import net.melbourne.utils.entity.player.PlayerUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.melbourne.utils.miscellaneous.Timer;
import net.melbourne.utils.rotation.RotationPoint;
import net.melbourne.utils.rotation.RotationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

@FeatureInfo(name = "AutoAnchor", category = Category.Combat)
public class AutoAnchorFeature extends Feature {

    public final NumberSetting range = new NumberSetting("Range", "Placement range", 5, 1, 6);
    public final NumberSetting targetRange = new NumberSetting("TargetRange", "Target Range", 12.0, 4.0, 20.0);
    public final NumberSetting minDamage = new NumberSetting("MinDamage", "Min Damage", 5.0, 0.0, 36.0);
    public final NumberSetting maxSelf = new NumberSetting("MaxSelf", "Max Self", 12.0, 0.0, 36.0);
    public final NumberSetting delay = new NumberSetting("Delay", "Action delay", 50, 0, 500);
    public final ModeSetting airPlace = new ModeSetting("AirPlace", "Air placement mode", "None", new String[]{"None", "Normal", "Grim"});
    public final ModeSetting switchMode = new ModeSetting("Switch", "Switching mode", "Silent", new String[]{"None", "Normal", "Silent"});
    public final BooleanSetting rotate = new BooleanSetting("Rotate", "Rotate on action", true);
    public final BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "Disable in Overworld", true);

    private final Timer timer = new Timer();
    private BlockPos pos = null;
    private PlayerEntity currentTarget = null;

    @Override
    public void onEnable() {
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    @Override
    public void onDisable() {
        Melbourne.EVENT_HANDLER.unsubscribe(this);
        pos = null;
        currentTarget = null;
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;
        if (autoDisable.getValue() && mc.world.getDimension().respawnAnchorWorks()) return;
        if (!timer.hasTimeElapsed(delay.getValue().longValue())) return;

        calculateBestPosition();

        if (pos != null) {
            doAnchorLogic(pos);
            timer.reset();
        }
    }

    private void calculateBestPosition() {
        pos = null;
        currentTarget = null;
        float maxDmg = 0;

        List<PlayerEntity> enemies = new ArrayList<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || Managers.FRIEND.isFriend(p.getName().getString())) continue;
            if (mc.player.squaredDistanceTo(p) > MathHelper.square(targetRange.getValue().floatValue())) continue;
            enemies.add(p);
        }

        if (enemies.isEmpty()) return;

        BlockPos existing = findBestExistingAnchor(enemies);
        if (existing != null) {
            pos = existing;
            return;
        }

//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        int r = (int) Math.ceil(range.getValue().floatValue());
        BlockPos base = mc.player.getBlockPos();


        //NIOGGA FUYCKASNDKJSAHNDJKSAHBNKJ
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = base.add(x, y, z);

                    if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > range.getValue().floatValue()) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    if (!state.isReplaceable()) continue;

                    if (airPlace.getValue().equals("None") && getHitResult(pos) == null) continue;
                    if (!mc.world.getOtherEntities(null, new Box(pos)).isEmpty()) continue;

                    for (PlayerEntity t : enemies) {
                        float dmg = CrystalUtils.calculateDamage(pos, t, t, true);
                        float self = CrystalUtils.calculateDamage(pos, mc.player, mc.player, true);

                        if (dmg < minDamage.getValue().floatValue()) continue;
                        //if (dmg != minDamage.getValue().floatValue()) continue;
                        if (self > maxSelf.getValue().floatValue()) continue;
                        if (self > PlayerUtils.getHealth(mc.player) - 1.0f) continue;

                        if (dmg > maxDmg) {
                            maxDmg = dmg;
                            this.pos = pos;
                            currentTarget = t;
                        }
                    }
                }
            }
        }
    }

    private BlockPos findBestExistingAnchor(List<PlayerEntity> targets) {
        int r = (int) Math.ceil(range.getValue().floatValue());
        BlockPos base = mc.player.getBlockPos();
        BlockPos best = null;
        float maxDmg = 0;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = base.add(x, y, z);
                    if (mc.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
                        for (PlayerEntity t : targets) {
                            float dmg = CrystalUtils.calculateDamage(pos, t, t, true);
                            float self = CrystalUtils.calculateDamage(pos, mc.player, mc.player, true);

                            if (dmg >= minDamage.getValue().floatValue() && self <= maxSelf.getValue().floatValue() && dmg > maxDmg) {
                                maxDmg = dmg;
                                best = pos;
                                currentTarget = t;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private void doAnchorLogic(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
            placeBlock(pos, Items.RESPAWN_ANCHOR);
        } else {
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges == 0) {
                interactBlock(pos, Items.GLOWSTONE);
            } else {
                interactBlock(pos, Items.AIR);
            }
        }
    }

    private void placeBlock(BlockPos pos, net.minecraft.item.Item item) {
        Slot slot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, item);
        if (slot == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        Services.INVENTORY.switchTo(slot.getIndex(), SwitchType.valueOf(switchMode.getValue()));

        BlockHitResult hit = getHitResult(pos);
        if (hit == null && !airPlace.getValue().equals("Nonwe")) {
            hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        }

        if (hit != null) {
            if (rotate.getValue()) rotateTo(hit.getPos());
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        if (switchMode.getValue().equals("Silent")) {
            Services.INVENTORY.switchTo(oldSlot, SwitchType.Silent);
        }
    }

    private void interactBlock(BlockPos pos, net.minecraft.item.Item item) {
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        boolean switched = false;

        if (item != Items.AIR) {
            Slot slot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, item);
            if (slot != null) {
                Services.INVENTORY.switchTo(slot.getIndex(), SwitchType.valueOf(switchMode.getValue()));
                switched = true;
            } else return;
        } else {
            if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                for (int i = 0; i < 9; i++) {
                    if (!mc.player.getInventory().getStack(i).isOf(Items.GLOWSTONE)) {
                        Services.INVENTORY.switchTo(i, SwitchType.valueOf(switchMode.getValue()));
                        switched = true;
                        break;
                    }
                }
            }
        }

        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        if (rotate.getValue()) rotateTo(hit.getPos());

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (switched && switchMode.getValue().equals("Silent")) {
            Services.INVENTORY.switchTo(oldSlot, SwitchType.Silent);
        }
    }

    private void rotateTo(Vec3d vec) {
        float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), vec);
        Services.ROTATION.setRotationPoint(new RotationPoint(rotations[0], rotations[1], 100, true));
    }

    private BlockHitResult getHitResult(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isReplaceable()) {
                return new BlockHitResult(Vec3d.ofCenter(pos), dir.getOpposite(), neighbor, false);
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (pos != null) {
            Renderer3D.renderBox(event.getContext(), new Box(pos), ColorUtils.getGlobalColor(80));
            Renderer3D.renderBoxOutline(event.getContext(), new Box(pos), ColorUtils.getGlobalColor(255));
        }
    }
}