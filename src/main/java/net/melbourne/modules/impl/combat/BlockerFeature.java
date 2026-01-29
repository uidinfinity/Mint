package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.services.impl.BreakService;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@FeatureInfo(name = "Blocker", category = Category.Combat)
public class BlockerFeature extends PlaceFeature {

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public final WhitelistSetting surroundBlock = new WhitelistSetting("Surround", "Types of surround blocking", WhitelistSetting.Type.CUSTOM, new String[]{"Above", "Beside"}, new String[]{"Above", "Beside"});
    public final ModeSetting cevBlock = new ModeSetting("Cev", "", "None", new String[]{"None", "Above"});
    public final ModeSetting civBlock = new ModeSetting("Civ", "", "None", new String[]{"None", "Above"});
    public final ModeSetting gravityBlock = new ModeSetting("Gravity", "Anti-gravity block protection", "None", new String[]{"None", "Feet"});
    public final ModeSetting brokenMode = new ModeSetting("Detection", "", "None", new String[]{"None", "Touched", "Broken"});
    public final BooleanSetting predict = new BooleanSetting("Predict", "Predict mining targets", false);

    private final Set<BlockPos> packetQueue = new LinkedHashSet<>();

    @Override
    public void onEnable() {
        super.onEnable();
        packetQueue.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || shouldDisable()) return;

        List<BlockPos> targets = new ArrayList<>(packetQueue);
        packetQueue.clear();

        handleManualDetection(targets);

        if (predict.getValue()) handlePredictions(targets);

        if (cevBlock.getValue().equals("Above")) {
            if (brokenMode.getValue().equals("None")) {
                BlockPos base = getCevBase();
                BlockPos target = getCevTarget();

                if (!mc.world.getBlockState(base).isAir()) {
                    if (mc.world.getBlockState(target).isAir()) {
                        targets.add(target);
                    }
                }
            }
        }

        if (gravityBlock.getValue().equals("Feet")) {
            if (isFallingBlockDetected()) {
                BlockPos feetPos = mc.player.getBlockPos();
                if (mc.world.getBlockState(feetPos).isAir()) {
                    targets.add(feetPos);
                }
            }
        }

        if (targets.isEmpty()) return;

        Slot obsidianSlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.OBSIDIAN, Items.ENDER_CHEST.canBeNested());

        List<BlockPos> distinctTargets = targets.stream().distinct().collect(Collectors.toList());

        for (BlockPos pos : distinctTargets) {
            if (gravityBlock.getValue().equals("Feet") && pos.equals(mc.player.getBlockPos())) {
                Slot antiGravSlot = findAntiGravitySlot();

                if (antiGravSlot != null) {
                    placeBlocks(getPath(List.of(pos)));
                    continue;
                }
            }

            if (obsidianSlot != null) {
                placeBlocks(getPath(List.of(pos)));
            }
        }
    }

    private Slot findAntiGravitySlot() {
        Item[] antiGravItems = {
                Items.STRING, Items.COBWEB, Items.OAK_BUTTON, Items.SPRUCE_BUTTON,
                Items.BIRCH_BUTTON, Items.JUNGLE_BUTTON, Items.ACACIA_BUTTON,
                Items.DARK_OAK_BUTTON, Items.MANGROVE_BUTTON, Items.CHERRY_BUTTON,
                Items.BAMBOO_BUTTON, Items.CRIMSON_BUTTON, Items.WARPED_BUTTON,
                Items.STONE_BUTTON, Items.POLISHED_BLACKSTONE_BUTTON
        };

        for (Item item : antiGravItems) {
            Slot slot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, item);
            if (slot != null) return slot;
        }
        return null;
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent e) {
        if (mc.player == null || mc.world == null) return;

        boolean wantBroken = brokenMode.getValue().equals("Broken");
        boolean wantPredict = predict.getValue();
        boolean wantCev = cevBlock.getValue().equals("Above");
        boolean passiveCev = wantCev && brokenMode.getValue().equals("None");

        if (!wantPredict && !wantBroken && !wantCev) return;
        if (!(e.getPacket() instanceof BlockUpdateS2CPacket p)) return;

        BlockPos pos = p.getPos();
        BlockState state = p.getState();

        if (wantCev) {
            BlockPos base = getCevBase();
            BlockPos target = getCevTarget();

            if (pos.equals(target)) {
                if (state.isAir()) {
                    if (!mc.world.getBlockState(base).isAir()) {
                        if (wantBroken || passiveCev) packetQueue.add(target);
                    }
                }
            }
            else if (pos.equals(base)) {
                if (!state.isAir() && passiveCev) {
                    if (mc.world.getBlockState(target).isAir()) packetQueue.add(target);
                }
            }
        }

        if (!wantPredict && !wantBroken && !passiveCev) return;
        if (!state.isAir()) return;

        if (isSurroundBlock(pos)) addSurroundReplacements(pos, packetQueue);
        if (isCivBlock(pos)) addCivReplacements(pos, packetQueue);
    }

    private boolean isFallingBlockDetected() {
        BlockPos base = mc.player.getBlockPos();
        for (int i = 1; i <= 3; i++) {
            if (mc.world.getBlockState(base.up(i)).getBlock() instanceof FallingBlock) return true;
        }

        Box box = new Box(
                mc.player.getX() - 0.5, mc.player.getY() + 1, mc.player.getZ() - 0.5,
                mc.player.getX() + 0.5, mc.player.getY() + 4, mc.player.getZ() + 0.5);

        for (Entity entity : mc.world.getOtherEntities(null, box)) {
            if (entity instanceof FallingBlockEntity) return true;
        }
        return false;
    }

    private void handlePredictions(List<BlockPos> targets) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.squaredDistanceTo(mc.player) > 36.0) continue;
            if (Managers.FRIEND.isFriend(player.getName().getString())) continue;

            BlockPos predicted = Services.SIMULATION.findSurroundMineTarget(player);
            if (predicted != null) {
                if (isSurroundBlock(predicted)) addSurroundReplacements(predicted, targets);

                if (cevBlock.getValue().equals("Above")) {
                    if (predicted.equals(getCevBase())) {
                        targets.add(getCevTarget());
                    }
                    if (predicted.equals(getCevTarget())) {
                        targets.add(getCevTarget());
                    }
                }

                if (isCivBlock(predicted)) addCivReplacements(predicted, targets);
            }
        }
    }

    private void handleManualDetection(List<BlockPos> targets) {
        BreakService breakService = Services.BREAK;
        String mode = brokenMode.getValue();

        if (mode.equals("None")) return;

        if (cevBlock.getValue().equals("Above")) {
            BlockPos base = getCevBase();
            BlockPos target = getCevTarget();
            boolean detected = false;

            if (!mc.world.getBlockState(base).isAir()) {
                if (mode.equals("Touched")) {
                    if (breakService.isBlockBeingMined(base)) detected = true;
                } else if (mode.equals("Broken")) {
                    if (mc.world.getBlockState(target).isAir()) detected = true;
                }
            }

            if (detected) targets.add(target);
        }

        if (!mode.equals("Touched")) return;

        List<BlockPos> surrounds = getSurround(mc.player);
        for (BlockPos pos : surrounds) {
            if (breakService.isBlockBeingMined(pos)) addSurroundReplacements(pos, targets);

            if (!civBlock.getValue().equals("None")) {
                if (breakService.isBlockBeingMined(pos.up())) addCivReplacements(pos.up(), targets);
                if (breakService.isBlockBeingMined(pos.up(2))) addCivReplacements(pos.up(2), targets);
            }
        }
    }

    private void addSurroundReplacements(BlockPos pos, List<BlockPos> targets) {
        targets.add(pos);
        if (surroundBlock.getWhitelistIds().contains("Above")) targets.add(pos.up());
        if (surroundBlock.getWhitelistIds().contains("Beside")) {
            for (Direction d : HORIZONTALS) targets.add(pos.offset(d));
        }
    }

    private void addSurroundReplacements(BlockPos pos, Set<BlockPos> targets) {
        targets.add(pos);
        if (surroundBlock.getWhitelistIds().contains("Above")) targets.add(pos.up());
        if (surroundBlock.getWhitelistIds().contains("Beside")) {
            for (Direction d : HORIZONTALS) targets.add(pos.offset(d));
        }
    }

    private void addCivReplacements(BlockPos pos, List<BlockPos> targets) {
        if (civBlock.getValue().equals("Above")) {
            BlockPos base = getBaseSurroundForCiv(pos);
            if (base != null) {
                targets.add(base.up());
                targets.add(base.up(2));
            }
        }
    }

    private void addCivReplacements(BlockPos pos, Set<BlockPos> targets) {
        if (civBlock.getValue().equals("Above")) {
            BlockPos base = getBaseSurroundForCiv(pos);
            if (base != null) {
                targets.add(base.up());
                targets.add(base.up(2));
            }
        }
    }

    private boolean isSurroundBlock(BlockPos pos) {
        return getSurround(mc.player).contains(pos);
    }

    private boolean isCivBlock(BlockPos pos) {
        return getBaseSurroundForCiv(pos) != null;
    }

    private BlockPos getBaseSurroundForCiv(BlockPos civPos) {
        List<BlockPos> surrounds = getSurround(mc.player);
        if (surrounds.contains(civPos.down())) return civPos.down();
        if (surrounds.contains(civPos.down(2))) return civPos.down(2);
        return null;
    }

    private List<BlockPos> getSurround(PlayerEntity p) {
        BlockPos base = p.getBlockPos();
        List<BlockPos> l = new ArrayList<>();
        for (Direction d : HORIZONTALS) l.add(base.offset(d));
        return l;
    }

    private BlockPos getCevBase() {
        return mc.player.getBlockPos().up(2);
    }

    private BlockPos getCevTarget() {
        return mc.player.getBlockPos().up(3);
    }
}