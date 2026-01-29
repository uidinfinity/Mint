package net.melbourne.modules.impl.combat;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.client.RendersFeature;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.block.BlockUtils;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.miscellaneous.NetworkUtils;
import net.melbourne.utils.miscellaneous.Timer;
import net.melbourne.utils.rotation.RotationPoint;
import net.melbourne.utils.rotation.RotationUtils;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@FeatureInfo(name = "PistonCrystal", category = Category.Combat)
public class PistonCrystalFeature extends Feature {

    private final ModeSetting breakType = new ModeSetting("Type", "Break Type", "Swing", new String[]{"Swing", "Packet"});
    private final ModeSetting placeMode = new ModeSetting("Place", "Placement Mode", "Torch", new String[]{"Torch", "Block", "Both"});
    private final ModeSetting switching = new ModeSetting("Switch", "Switch mode.", "Silent", new String[]{"None", "Normal", "Silent", "Swap", "Pickup"});

    private final NumberSetting enemyRange = new NumberSetting("Range", "Enemy Range", 4.9, 0, 6);
    private final NumberSetting startDelay = new NumberSetting("StartDelay", "Start Delay", 4, 0, 20);
    private final NumberSetting pistonDelay = new NumberSetting("PistonDelay", "Piston Delay", 2, 0, 20);
    private final NumberSetting crystalDelay = new NumberSetting("CrystalDelay", "Crystal Delay", 2, 0, 20);
    private final NumberSetting hitDelay = new NumberSetting("HitDelay", "Hit Delay", 2, 0, 20);
    private final NumberSetting maxYincr = new NumberSetting("MaxY", "Max Y height", 3, 0, 5);
    private final NumberSetting stuckThreshold = new NumberSetting("StuckLimit", "Max ticks to wait if stuck", 10, 5, 40);

    private final BooleanSetting rotate = new BooleanSetting("Rotate", "Rotate", true);
    private final BooleanSetting confirmBreak = new BooleanSetting("NoGlitchBreak", "Wait for break confirmation", true);
    private final BooleanSetting confirmPlace = new BooleanSetting("NoGlitchPlace", "Wait for place confirmation", true);
    private final BooleanSetting debugMode = new BooleanSetting("Debug", "Debug messages", false);
    private final BooleanSetting render = new BooleanSetting("Render", "Render the structure", true);

    private final Timer timer = new Timer();
    private final Timer hitTimer = new Timer();

    private boolean noMaterials, isHole, enoughSpace, redstoneBlockMode, broken;
    private int stage;
    private int redstoneTickDelay;
    private int stuckTicks = 0;

    private int[] delayTable;
    private BlockPos enemyCoordsInt;
    private Vec3d enemyCoordsDouble;

    private StructureTemp toPlace;
    private PlayerEntity aimTarget;

    @Override
    public void onEnable() {
        if (getNull()) {
            setToggled(false);
            return;
        }
        initValues();
        findTarget();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void initValues() {
        delayTable = new int[]{startDelay.getValue().intValue(), 0, pistonDelay.getValue().intValue(), crystalDelay.getValue().intValue(), hitDelay.getValue().intValue()};
        reset();
    }

    private void reset() {
        toPlace = null;
        aimTarget = null;
        isHole = true;
        broken = false;
        stage = 0;
        stuckTicks = 0;
        timer.reset();
        hitTimer.reset();
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;

        int currentDelay = delayTable[Math.min(stage, delayTable.length - 1)];
        if (!timer.hasTimeElapsed(currentDelay * 50L)) return;

        if (aimTarget == null || !aimTarget.isAlive() || mc.player.distanceTo(aimTarget) > enemyRange.getValue().floatValue()) {
            if (!findTarget()) {
                setToggled(false);
                return;
            }
        }

        if (stage == 0) {
            playerChecks();
            if (noMaterials || !isHole || !enoughSpace) return;
            if (toPlace != null && toPlace.to_place != null) {
                stage = 1;
                stuckTicks = 0;
                timer.reset();
            }
        } else {
            stuckTicks++;
            if (stuckTicks > stuckThreshold.getValue().intValue() * 20) {
                reset();
                return;
            }

            switch (stage) {
                case 1 -> {
                    if (confirmBreak.getValue() && (checkCrystalPlaceExt() || checkCrystalPlaceIns() != null)) {
                        stage = 4;
                        break;
                    }
                    if (checkPistonPlace()) {
                        stage++;
                        stuckTicks = 0;
                        timer.reset();
                        return;
                    }
                    placeBlockThings(1);
                }
                case 2 -> {
                    if (checkCrystalPlaceExt()) {
                        stage++;
                        stuckTicks = 0;
                        timer.reset();
                        return;
                    }
                    placeBlockThings(0);
                }
                case 3 -> {
                    if (redstoneTickDelay++ >= 0) {
                        redstoneTickDelay = 0;
                        if (checkRedstonePlace()) {
                            stage++;
                            stuckTicks = 0;
                            timer.reset();
                            return;
                        }
                        placeBlockThings(2);
                    }
                }
                case 4 -> {
                    destroyCrystalAlgo();
                    if (broken) {
                        stage = 0;
                        toPlace = null;
                        timer.reset();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull()) return;
        if (event.getPacket() instanceof PlaySoundS2CPacket packet) {
            if (packet.getCategory() == SoundCategory.BLOCKS && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                if (enemyCoordsInt != null && (int) packet.getX() == enemyCoordsInt.getX() && (int) packet.getZ() == enemyCoordsInt.getZ()) {
                    stage = 0;
                    toPlace = null;
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (!render.getValue() || toPlace == null || enemyCoordsDouble == null) return;
        RendersFeature renders = Managers.FEATURE.getFeatureFromClass(RendersFeature.class);
        if (renders == null || !renders.isEnabled()) return;

        Color c = renders.getBlockColor();
        for (Vec3d vec : toPlace.to_place) {
            if (vec == null) continue;
            BlockPos pos = new BlockPos((int) (enemyCoordsDouble.x + vec.x), (int) (enemyCoordsDouble.y + vec.y), (int) (enemyCoordsDouble.z + vec.z));
            Renderer3D.renderBox(event.getContext(), new Box(pos), c);
            Renderer3D.renderBoxOutline(event.getContext(), new Box(pos), c);
        }
    }

    private void placeBlockThings(int step) {
        if (toPlace.to_place == null || step >= toPlace.to_place.size()) return;
        doPlace(compactBlockPos(step), step);
    }

    private Direction dirFromDelta(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private float yawForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> mc.player.getYaw();
        };
    }

    private static boolean spoofingRot;

    private void sendLook(float yaw, float pitch) {
        float prevYaw = mc.player.getYaw();
        float prevPitch = mc.player.getPitch();

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);

        spoofingRot = true;
        ((net.melbourne.mixins.accessors.IClientPlayerEntity) mc.player).melbourne$sendMovementPackets();
        spoofingRot = false;

        Services.ROTATION.setRotationPoint(new RotationPoint(yaw, pitch, 10000, true));

        mc.player.setYaw(prevYaw);
        mc.player.setPitch(prevPitch);
    }



    public void doPlace(BlockPos pos, int step) {
        Item itemToPlace = getItemFromSlotIndex(step);
        Slot targetSlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, itemToPlace);
        if (targetSlot == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        Services.INVENTORY.switchTo(targetSlot.getIndex(), getSwitchType());

        if (switching.getValue().equalsIgnoreCase("Pickup")) {
            int syncId = mc.player.currentScreenHandler.syncId;
            int revision = mc.player.currentScreenHandler.getRevision();
            ItemStack stack = mc.player.getInventory().getStack(targetSlot.getIndex());
            ItemStackHash hash = ItemStackHash.fromItemStack(stack, mc.getNetworkHandler().getComponentHasher());
            Int2ObjectMap<ItemStackHash> map = new Int2ObjectArrayMap<>();
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncId, revision, (short) targetSlot.getIndex(), (byte) 0, SlotActionType.PICKUP, map, hash));
        }

        BlockPos interactPos = pos.down();
        Direction side = Direction.UP;

        BlockHitResult bhr = new BlockHitResult(interactPos.toCenterPos(), side, interactPos, false);

        if (step == 1) {
            BlockPos crystalPos = compactBlockPos(0);

            int dx = crystalPos.getX() - pos.getX();
            int dz = crystalPos.getZ() - pos.getZ();

            Direction wantFace = dirFromDelta(dx, dz);
            Direction spoofLook = wantFace.getOpposite();

            float yaw = yawForFacing(spoofLook);
            float pitch = 0f;

            sendLook(yaw, pitch);
            NetworkUtils.sendSequencedPacket(s -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, s));
        } else {
            if (rotate.getValue()) {
                float[] rots = RotationUtils.getRotationsTo(mc.player.getEyePos(), pos.toCenterPos());
                sendLook(rots[0], rots[1]);
            }
            NetworkUtils.sendSequencedPacket(s -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, bhr, s));
        }

        if (breakType.getValue().equalsIgnoreCase("Swing")) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        Services.INVENTORY.switchTo(oldSlot, SwitchType.Silent);
    }

    private void destroyCrystalAlgo() {
        Entity crystal = checkCrystalPlaceIns();
        if (crystal != null && hitTimer.hasTimeElapsed(hitDelay.getValue().intValue() * 50L)) {
            hitTimer.reset();
            if (rotate.getValue()) {
                float[] rots = RotationUtils.getRotationsTo(mc.player.getEyePos(), crystal.getPos());
                Services.ROTATION.setRotationPoint(new RotationPoint(rots[0], rots[1], 100, true));
            }
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            if (breakType.getValue().equalsIgnoreCase("Swing")) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            broken = true;
        } else {
            broken = false;
        }
    }

    private boolean findTarget() {
        double dist = enemyRange.getValue().floatValue();
        aimTarget = null;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || Managers.FRIEND.isFriend(p.getName().getString())) continue;
            if (mc.player.distanceTo(p) <= dist) {
                aimTarget = p;
                dist = mc.player.distanceTo(p);
            }
        }
        return aimTarget != null;
    }

    private void playerChecks() {
        noMaterials = !getMaterialsSlot();
        if (aimTarget != null) {
            isHole = HoleUtils.isHole(aimTarget.getBlockPos());
            if (isHole) {
                enemyCoordsDouble = aimTarget.getPos();
                enemyCoordsInt = aimTarget.getBlockPos();
                enoughSpace = createStructure();
            }
        }
    }

    private boolean createStructure() {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] off : offsets) {
            BlockPos crystalPos = enemyCoordsInt.add(off[0], 1, off[1]);
            BlockPos pistonPos = enemyCoordsInt.add(off[0] * 2, 1, off[1] * 2);
            BlockPos redstonePos = enemyCoordsInt.add(off[0] * 3, 1, off[1] * 3);

            if (BlockUtils.isAir(crystalPos) && BlockUtils.isAir(pistonPos) && BlockUtils.isAir(redstonePos)) {
                List<Vec3d> structure = new ArrayList<>();
                structure.add(new Vec3d(off[0], 1, off[1]));
                structure.add(new Vec3d(off[0] * 2, 1, off[1] * 2));
                structure.add(new Vec3d(off[0] * 3, 1, off[1] * 3));
                toPlace = new StructureTemp(structure);
                return true;
            }
        }
        return false;
    }

    private Item getItemFromSlotIndex(int step) {
        return switch (step) {
            case 0 -> Items.END_CRYSTAL;
            case 1 -> Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.PISTON) != null ? Items.PISTON : Items.STICKY_PISTON;
            case 2 -> redstoneBlockMode ? Items.REDSTONE_BLOCK : Items.REDSTONE_TORCH;
            default -> Items.AIR;
        };
    }

    private BlockPos compactBlockPos(int step) {
        Vec3d rel = toPlace.to_place.get(step);
        return new BlockPos((int) (enemyCoordsDouble.x + rel.x), (int) (enemyCoordsDouble.y + rel.y), (int) (enemyCoordsDouble.z + rel.z));
    }

    private boolean checkPistonPlace() {
        return mc.world.getBlockState(compactBlockPos(1)).getBlock() instanceof PistonBlock;
    }

    private boolean checkRedstonePlace() {
        return mc.world.getBlockState(compactBlockPos(2)).getBlock() instanceof RedstoneBlock || mc.world.getBlockState(compactBlockPos(2)).getBlock() instanceof RedstoneTorchBlock;
    }

    private boolean checkCrystalPlaceExt() {
        BlockPos p = compactBlockPos(0);
        return !mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(p), e -> true).isEmpty();
    }

    private Entity checkCrystalPlaceIns() {
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, aimTarget.getBoundingBox().expand(1.5), e -> true).stream().findFirst().orElse(null);
    }

    private boolean getMaterialsSlot() {
        boolean hasPiston = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.PISTON) != null || Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.STICKY_PISTON) != null;
        boolean hasCrystal = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.END_CRYSTAL) != null;
        redstoneBlockMode = placeMode.getValue().equals("Block") && Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.REDSTONE_BLOCK) != null;
        boolean hasRedstone = redstoneBlockMode || Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.REDSTONE_TORCH) != null;
        return hasPiston && hasCrystal && hasRedstone;
    }

    private SwitchType getSwitchType() {
        return switch (switching.getValue()) {
            case "Normal" -> SwitchType.Normal;
            case "Silent" -> SwitchType.Silent;
            case "Swap" -> SwitchType.Swap;
            case "Pickup" -> SwitchType.PickUp;
            default -> SwitchType.None;
        };
    }

    private record StructureTemp(List<Vec3d> to_place) {}
}
