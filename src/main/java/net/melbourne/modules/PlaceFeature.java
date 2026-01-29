package net.melbourne.modules;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.melbourne.Melbourne;
import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.impl.client.RendersFeature;
import net.melbourne.pathfinding.placement.PlacePathfinding;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.miscellaneous.NetworkUtils;
import net.melbourne.utils.miscellaneous.Timer;
import net.melbourne.utils.rotation.RotationPoint;
import net.melbourne.utils.rotation.RotationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceFeature extends Feature {

    public final NumberSetting blocksPerTick = new NumberSetting("BlocksPerTick", "Blocks per tick", 4, 1, 8);
    public final NumberSetting delay = new NumberSetting("Delay", "Delay in ticks between placement batches", 0, 0, 6);
    public final NumberSetting placeRange = new NumberSetting("PlaceRange", "Placement range", 5, 0, 6);
    public final NumberSetting maxSupport = new NumberSetting("MaxSupport", "Maximum support block count.", 2, 1, 5);
    public final ModeSetting timing = new ModeSetting("Timing", "Timing mode", "Vanilla", new String[]{"Vanilla", "Sequential"});
    public final ModeSetting swing = new ModeSetting("Swing", "Swing mode", "None", new String[]{"None", "Packet", "Mainhand", "Offhand"});
    public final ModeSetting switchMode = new ModeSetting("Switch", "Switching mode", "Silent", new String[]{"None", "Normal", "Silent"});
    public final ModeSetting airPlace = new ModeSetting("AirPlace", "Air placement mode", "None", new String[]{"None", "Normal", "Grim"});
    public final WhitelistSetting toggles = new WhitelistSetting("Toggles", "Module toggle conditions", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Jump", "Chorus", "Item"});

    public final BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Face directions only", false);
    public final BooleanSetting rotate = new BooleanSetting("Rotate", "Rotate on place", true);
    public final BooleanSetting breakCrystals = new BooleanSetting("BreakCrystals", "Break obstructing crystals", true);
    public final BooleanSetting whileUsing = new BooleanSetting("WhileUsing", "Place while eating", true);
    public final BooleanSetting render = new BooleanSetting("Render", "Render placement", true);

    private final PlacePathfinding pathfinder = new PlacePathfinding();
    private final ConcurrentHashMap<BlockPos, Long> renderBlocks = new ConcurrentHashMap<>();
    private int placedBlocks;
    private final Timer delayTimer = new Timer();
    protected final Set<BlockPos> sequentialPositions = new HashSet<>();
    public double startY;
    protected Item[] currentItems = new Item[]{Items.OBSIDIAN, Items.ENDER_CHEST};

    public PlaceFeature() {
        Collections.addAll(getSettings(), blocksPerTick, delay, placeRange, maxSupport, timing, swing, switchMode, airPlace, toggles, strictDirection, rotate, breakCrystals, whileUsing, render);
    }

    @Override
    public void onEnable() {
        placedBlocks = 0;
        renderBlocks.clear();
        sequentialPositions.clear();
        delayTimer.reset();
        if (mc.player != null) startY = mc.player.getY();
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    @Override
    public void onDisable() {
        Melbourne.EVENT_HANDLER.unsubscribe(this);
    }

    public List<BlockPos> getPath(Collection<BlockPos> positions) {
        LinkedHashSet<BlockPos> out = new LinkedHashSet<>();
        for (BlockPos pos : positions) out.addAll(getPath(pos));
        return new ArrayList<>(out);
    }

    public List<BlockPos> getPath(BlockPos pos) {
        return pathfinder.findPathToPlace(pos, placeRange.getValue().floatValue(), maxSupport.getValue().intValue());
    }

    public boolean placeBlocks(Collection<BlockPos> positions) {
        return placeBlocks(positions, Items.OBSIDIAN, Items.ENDER_CHEST);
    }

    public boolean placeBlocks(Collection<BlockPos> positions, Item... items) {
        if (shouldDisable() || positions == null || positions.isEmpty()) return false;
        if (!delayTimer.hasTimeElapsed(delay.getValue().intValue() * 50L)) return false;

        this.currentItems = items;
        placedBlocks = 0;

        Slot slot = null;
        for (Item item : items) {
            slot = Services.INVENTORY.findSlot(SearchLogic.All, item);
            if (slot != null) break;
        }
        if (slot == null) return false;

        SwitchType type = SwitchType.valueOf(switchMode.getValue());
        Services.INVENTORY.switchTo(slot, true, type);

        int max = blocksPerTick.getValue().intValue();
        boolean placedAny = false;

//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        ArrayList<BlockPos> targets = new ArrayList<>(new LinkedHashSet<>(positions));
        targets.sort(Comparator.comparingDouble(p ->
                mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)));

        for (BlockPos target : targets) {
            if (placedBlocks >= max) break;
            if (mc.player == null || mc.world == null) break;

            if (!mc.world.getBlockState(target).isReplaceable()) continue;
            if (mc.player.squaredDistanceTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5)
                    > Math.pow(placeRange.getValue().floatValue(), 2)) continue;

            if (needsDownSupport(target)) {
                if (max - placedBlocks >= 2) {
                    if (placeDownSupportThenTarget(target, max)) placedAny = true;
                    continue;
                } else {
                    BlockPos below = target.down();
                    if (mc.world.getBlockState(below).isReplaceable() && getHitResult(below) != null) {
                        if (performPlace(below)) {
                            placedBlocks++;
                            placedAny = true;
                        }
                    } else {
                        if (performPlace(target)) {
                            placedBlocks++;
                            placedAny = true;
                        }
                    }
                    continue;
                }
            }

            if (performPlace(target)) {
                placedBlocks++;
                placedAny = true;
            } else if (airPlace.getValue().equalsIgnoreCase("None")) {
                List<BlockPos> path = pathfinder.findPathToPlace(
                        target,
                        placeRange.getValue().floatValue(),
                        maxSupport.getValue().intValue()
                );

                if (path != null && !path.isEmpty()) {
                    for (BlockPos p : path) {
                        if (placedBlocks >= max) break;
                        if (mc.world.getBlockState(p).isReplaceable()) {
                            if (performPlace(p)) {
                                placedBlocks++;
                                placedAny = true;
                            }
                        }
                    }
                }
            }
        }

        Services.INVENTORY.switchBack();
        if (placedAny) delayTimer.reset();
        return placedAny;
    }


    private boolean needsDownSupport(BlockPos target) {
        if (mc.world == null) return false;
        if (!mc.world.getBlockState(target).isReplaceable()) return false;
        if (!airPlace.getValue().equalsIgnoreCase("None")) return false;

        BlockPos below = target.down();
        boolean floating = mc.world.getBlockState(below).isReplaceable();
        boolean noFace = getHitResult(target) == null;

        return floating || noFace;
    }

    private boolean placeDownSupportThenTarget(BlockPos target, int max) {
        if (mc.player == null || mc.world == null) return false;
        if (!mc.world.getBlockState(target).isReplaceable()) return false;

        boolean placedAny = false;

        BlockPos below = target.down();

        if (placedBlocks < max && mc.world.getBlockState(below).isReplaceable()) {
            BlockHitResult belowRes = getHitResult(below);
            if (belowRes != null) {
                if (performPlace(below)) {
                    placedBlocks++;
                    placedAny = true;
                }
            }
        }

        if (placedBlocks < max) {
            if (performPlace(target)) {
                placedBlocks++;
                placedAny = true;
            }
        }

        return placedAny;
    }



    private LinkedHashSet<BlockPos> buildSupportOrderedList(Collection<BlockPos> input) {
        LinkedHashSet<BlockPos> out = new LinkedHashSet<>();
        if (mc.player == null || mc.world == null) return out;

        for (BlockPos target : input) {
            if (target == null) continue;

            boolean direct = !airPlace.getValue().equalsIgnoreCase("None") || getHitResult(target) != null;
            if (direct) {
                out.add(target);
                continue;
            }

            List<BlockPos> path = pathfinder.findPathToPlace(target, placeRange.getValue().floatValue(), maxSupport.getValue().intValue());
            if (path != null && !path.isEmpty()) {
                for (BlockPos p : path) out.add(p);
            } else {
                BlockPos support = target.down();
                out.add(support);
                out.add(target);
            }
        }

        return out;
    }

    public boolean performPlace(BlockPos pos) {
        if (mc.player == null || mc.world == null || !mc.world.getBlockState(pos).isReplaceable()) return false;
        if (mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > Math.pow(placeRange.getValue().floatValue(), 2)) return false;
        if (!whileUsing.getValue() && mc.player.isUsingItem()) return false;

        // GRIMM AIRPLACE
        if (airPlace.getValue().equalsIgnoreCase("Grim")) {
            int ogSlot = mc.player.getInventory().getSlotWithStack(mc.player.getMainHandStack());

            int screenSlot = ogSlot + 36;
            ItemStack stack = mc.player.getMainHandStack();

            ItemStackHash hash = ItemStackHash.fromItemStack(stack, mc.getNetworkHandler().getComponentHasher());
            Int2ObjectMap<ItemStackHash> map = new Int2ObjectArrayMap<>();

            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(0, 0, (short) screenSlot, (byte) 0, SlotActionType.SWAP, map, hash));
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(0));

            int slot36 = 36;
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(0, 0, (short) slot36, (byte) 0, SlotActionType.PICKUP, map, hash));

            BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(0, 0, (short) slot36, (byte) 0, SlotActionType.PICKUP, map, hash));
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(0, 0, (short) slot36, (byte) 0, SlotActionType.PICKUP, map, hash));

            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.swingHand(Hand.MAIN_HAND);

            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(0, 0, (short) screenSlot, (byte) 0, SlotActionType.SWAP, map, hash));

            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(ogSlot));

            if (render.getValue()) renderBlocks.put(pos, System.currentTimeMillis());
            return true;
        }

        BlockHitResult result = getHitResult(pos);
        if (result == null) return false;

        if (rotate.getValue()) {
            float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), result.getPos());
            Services.ROTATION.setRotationPoint(new RotationPoint(rotations[0], rotations[1], 10000, true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    rotations[0],
                    rotations[1],
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
            ));
        }

        breakObstructingCrystal(pos);

        boolean isGrim = airPlace.getValue().equalsIgnoreCase("Grim");
        int syncId = mc.player.currentScreenHandler.syncId;
        int revision = mc.player.currentScreenHandler.getRevision();
        int slot = 36 + mc.player.getInventory().getSlotWithStack(mc.player.getMainHandStack());
        ItemStack stack = mc.player.getMainHandStack();

        if (isGrim) {
            ItemStackHash hash = ItemStackHash.fromItemStack(stack, mc.getNetworkHandler().getComponentHasher());
            Int2ObjectMap<ItemStackHash> map = new Int2ObjectArrayMap<>();
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncId, revision, (short) slot, (byte) 0, SlotActionType.PICKUP, map, hash));
        }

        NetworkUtils.sendSequencedPacket(s -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, s));

        if (isGrim) {
            ItemStackHash hash = ItemStackHash.fromItemStack(stack, mc.getNetworkHandler().getComponentHasher());
            Int2ObjectMap<ItemStackHash> map = new Int2ObjectArrayMap<>();
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncId, revision, (short) slot, (byte) 0, SlotActionType.PICKUP, map, hash));
            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(syncId, revision, (short) slot, (byte) 0, SlotActionType.PICKUP, map, hash));
        }

        switch (swing.getValue()) {
            case "Packet" -> mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            case "Mainhand" -> mc.player.swingHand(Hand.MAIN_HAND);
            case "Offhand" -> mc.player.swingHand(Hand.OFF_HAND);
        }

        if (render.getValue()) renderBlocks.put(pos, System.currentTimeMillis());
        return true;
    }

    public boolean canPlace(BlockPos pos) {
        if (mc.player == null || mc.world == null || !mc.world.getBlockState(pos).isReplaceable()) return false;
        if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > Math.pow(placeRange.getValue().floatValue(), 2)) return false;
        if (!whileUsing.getValue() && mc.player.isUsingItem()) return false;
        if (!airPlace.getValue().equalsIgnoreCase("None")) return true;
        if (getHitResult(pos) != null) return true;
        BlockPos support = pos.down();
        return mc.world.getBlockState(support).isReplaceable() && getHitResult(support) != null;
    }

    public BlockHitResult getHitResult(BlockPos pos) {
        Vec3d eyes = mc.player.getEyePos();
        BlockPos below = pos.down();
        if (!mc.world.getBlockState(below).isReplaceable()) {
            BlockHitResult quickRes = getSpecificHitResult(pos, Direction.DOWN, eyes);
            if (quickRes != null) return quickRes;
        }
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            BlockHitResult res = getSpecificHitResult(pos, dir, eyes);
            if (res != null) return res;
        }
        if (!airPlace.getValue().equalsIgnoreCase("None")) {
            return new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        }
        return null;
    }

    private BlockHitResult getSpecificHitResult(BlockPos pos, Direction dir, Vec3d eyes) {
        BlockPos neighbor = pos.offset(dir);
        BlockState state = mc.world.getBlockState(neighbor);
        if (state.isAir() || state.isReplaceable()) return null;
        Direction side = dir.getOpposite();
        Vec3d hitVec = Vec3d.ofCenter(pos).add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5);
        if (strictDirection.getValue()) {
            Vec3d eyeToHit = hitVec.subtract(eyes);
            if (eyeToHit.dotProduct(Vec3d.of(side.getVector())) >= 0) return null;
        }
        return new BlockHitResult(hitVec, side, neighbor, false);
    }

    public void breakObstructingCrystal(BlockPos pos) {
        if (!breakCrystals.getValue() || mc.world == null) return;
        List<Entity> crystals = mc.world.getOtherEntities(null, new Box(pos), e -> e instanceof EndCrystalEntity);
        for (Entity crystal : crystals) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }

    public boolean shouldDisable() {
        if (mc.player == null || mc.world == null) return false;
        if (toggles.getWhitelistIds().contains("Jump") && shouldDisableOnJump()) {
            setToggled(false);
            return true;
        }
        if (toggles.getWhitelistIds().contains("Item")) {
            int count = Services.INVENTORY.getCount(v -> {
                for (Item i : currentItems) if (v.getItem() == i) return true;
                return false;
            });
            if (count <= 0) {
                setToggled(false);
                return true;
            }
        }
        return false;
    }

    private boolean shouldDisableOnJump() {
        return !mc.player.isInLava() && !mc.player.isSubmergedInWater() && (!mc.player.isOnGround() || Math.abs(mc.player.getY() - startY) > 0.1);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (timing.getValue().equals("Sequential")) {
            if (event.getPacket() instanceof BlockUpdateS2CPacket packet) {
                if (packet.getState().isAir() && sequentialPositions.contains(packet.getPos())) {
                    if (placedBlocks < blocksPerTick.getValue().intValue()) {
                        if (performPlace(packet.getPos())) placedBlocks++;
                    }
                }
            }
        }
        if (event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getEntity(mc.world) == mc.player && packet.getStatus() == 46) {
                if (toggles.getWhitelistIds().contains("Chorus")) setToggled(false);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (!render.getValue() || mc.world == null) return;
        RendersFeature renders = Managers.FEATURE.getFeatureFromClass(RendersFeature.class);
        if (renders == null || !renders.isEnabled()) return;
        long now = System.currentTimeMillis();
        long lifetime = renders.getRenderTimeMillis();
        Color base = renders.getBlockColor();
        renderBlocks.entrySet().removeIf(e -> (now - e.getValue()) > lifetime);
        renderBlocks.forEach((pos, time) -> {
            float progress = 1.0f - ((now - time) / (float) lifetime);
            Box box = new Box(pos);
            Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (base.getAlpha() * progress));
            Renderer3D.renderBox(event.getContext(), box, fill);
        });
    }
}