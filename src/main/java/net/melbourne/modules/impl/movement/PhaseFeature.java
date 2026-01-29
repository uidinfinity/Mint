package net.melbourne.modules.impl.movement;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

@FeatureInfo(name = "Phase", category = Category.Movement)
public class PhaseFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "Phasing method", "Pearl", new String[]{"Pearl", "Packet"});
    public ModeSetting shift = new ModeSetting("Shift", "Action when sneaking", "None", new String[]{"None", "Move", "Descend"},
            () -> mode.getValue().equalsIgnoreCase("Packet"));
    public ModeSetting space = new ModeSetting("Space", "Action when jumping", "None", new String[]{"None", "Ascend"},
            () -> mode.getValue().equalsIgnoreCase("Packet"));
    public NumberSetting limit = new NumberSetting("Limit", "Blocks per second limit (0 = No limit)", 0, 0, 10,
            () -> mode.getValue().equalsIgnoreCase("Packet"));
    public BooleanSetting attack = new BooleanSetting("Attack", "Attack entities blocking pearl phase", true,
            () -> mode.getValue().equalsIgnoreCase("Pearl"));

    private int originalSlot = -1;
    private final Timer limitTimer = new Timer();

    private int progress = 0;
    private double ascendTotal = 0;

    @Override
    public void onEnable() {
        if (getNull()) return;

        progress = 0;
        ascendTotal = mc.player.getY();

        if (mode.getValue().equals("Pearl")) {
            doPearlPhase();
        }
    }

    @Override
    public void onDisable() {
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull() || !mode.getValue().equals("Packet")) return;

        boolean isSneaking = mc.options.sneakKey.isPressed();
        boolean isJumping = mc.options.jumpKey.isPressed();

        boolean isForward = mc.options.forwardKey.isPressed();
        boolean isBack = mc.options.backKey.isPressed();
        boolean isLeft = mc.options.leftKey.isPressed();
        boolean isRight = mc.options.rightKey.isPressed();

        boolean isMovingHorizontally = isForward || isBack || isLeft || isRight;
        boolean isMovingVertically = (isJumping && space.getValue().equals("Ascend")) || (isSneaking && shift.getValue().equals("Descend"));

        if (shift.getValue().equals("Move") && !isSneaking) {
            return;
        }

        if (!isMovingHorizontally && !isMovingVertically) return;

        if (shift.getValue().equals("None") && !isJumping) {
            if (isSneaking) {
                return;
            }
        }

        if (!isInsideBlock()) {
            progress = 0;
            ascendTotal = mc.player.getY();
            return;
        }

        if (limit.getValue().intValue() != 0) {
            long msLimit = 1000 / limit.getValue().intValue();
            if (!limitTimer.passed(msLimit)) return;
        }

        Vec3d phasePos = mc.player.getPos();
        boolean moved = false;

        if (isJumping && space.getValue().equals("Ascend")) {
            double offset = Math.min(progress * 0.06, 1.0);
            phasePos = new Vec3d(mc.player.getX(), ascendTotal + offset, mc.player.getZ());
            progress++;
            moved = true;
        } else if (isSneaking && shift.getValue().equals("Descend")) {
            double targetY = mc.player.getY() - 0.0253;

            if (targetY > mc.world.getBottomY() && !isAboveVoid()) {
                phasePos = phasePos.add(0.0, -0.0253, 0.0);
                moved = true;
            }
        } else if (isMovingHorizontally) {
            double yawRad = Math.toRadians(mc.player.getYaw());

            double forward = isForward ? 1 : (isBack ? -1 : 0);
            double strafe = isLeft ? 1 : (isRight ? -1 : 0);

            double xDir = (forward * -Math.sin(yawRad) + strafe * Math.cos(yawRad)) * 0.005;
            double zDir = (forward * Math.cos(yawRad) - strafe * -Math.sin(yawRad)) * 0.005;

            phasePos = phasePos.add(xDir, 0, zDir);
            progress = 0;
            ascendTotal = mc.player.getY();
            moved = true;
        }

        if (moved) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(phasePos.x, phasePos.y, phasePos.z, mc.player.isOnGround(), true));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(phasePos.x, phasePos.y - 87.0, phasePos.z, mc.player.isOnGround(), true));

            mc.player.setPosition(phasePos);
            limitTimer.reset();
        }
    }

    private boolean isAboveVoid() {
        for (int i = 1; i <= 20; i++) {
            BlockPos checkPos = mc.player.getBlockPos().down(i);
            if (checkPos.getY() < mc.world.getBottomY()) return true;
            if (!mc.world.getBlockState(checkPos).isAir()) return false;
        }
        return true;
    }

    private boolean isInsideBlock() {
        Box box = mc.player.getBoundingBox().contract(0.0625);

        for (BlockPos pos : BlockPos.iterate(MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ), MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ)
        )) {
            if (!mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void doPearlPhase() {
        ClientPlayerEntity player = mc.player;

        ItemStack pearlStack = findInventoryItemStack(Items.ENDER_PEARL);

        if (player.getItemCooldownManager().isCoolingDown(pearlStack)) {
            setToggled(false);
            return;
        }

        originalSlot = player.getInventory().getSelectedSlot();
        float originalPitch = player.getPitch();
        float originalYaw = player.getYaw();

        Vec3d targetPos = new Vec3d(Math.floor(player.getX()) + 0.5, 0.0, Math.floor(player.getZ()) + 0.5);
        float[] rotations = getRotationsTo(player.getEyePos(), targetPos);
        float yaw = rotations[0];

        if (attack.getValue()) {
            BlockHitResult hitResult = rayCast(3.0, yaw + 180.0f, 60.0f);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                Box box = new Box(hitResult.getBlockPos()).expand(0.2);
                for (Entity entity : mc.world.getOtherEntities(null, box)) {
                    if (entity instanceof ItemFrameEntity itemFrame && !itemFrame.getHeldItemStack().isEmpty()) {
                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking()));
                    } else if (entity instanceof EndCrystalEntity) {
                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking()));
                    }
                    mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                }
            }

            BlockPos playerPos = player.getBlockPos();
            if (mc.world.getBlockState(playerPos).getBlock() instanceof ScaffoldingBlock) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, playerPos, Direction.UP));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, playerPos, Direction.UP));
            }
        }

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw + 180.0f, 85.0f, player.isOnGround(), true));

        int pearlSlot = findHotbarItemSlot(Items.ENDER_PEARL);
        if (pearlSlot == -1) {
            pearlSlot = silentSwapPearl();
            if (pearlSlot == -1) {
                setToggled(false);
                return;
            }
        }

        player.getInventory().setSelectedSlot(pearlSlot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));
        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, getNextSequenceId(), yaw + 180.0f, 85.0f));
        player.swingHand(Hand.MAIN_HAND);

        player.getInventory().setSelectedSlot(originalSlot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        restoreInventory();
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(originalYaw, originalPitch, player.isOnGround(), true));

        setToggled(false);
    }

    private float[] getRotationsTo(Vec3d from, Vec3d to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));
        return new float[]{
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(difZ, difX)) - 90.0f),
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(difY, dist)))
        };
    }

    private ItemStack findInventoryItemStack(net.minecraft.item.Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private int findHotbarItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private int silentSwapPearl() {
        int targetSlot = -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                targetSlot = invIndexToHandlerId(i);
                break;
            }
        }
        if (targetSlot == -1) return -1;

        int hotbarSlot = findEmptyHotbar();
        if (hotbarSlot == -1) hotbarSlot = 8;
        int hotbarId = invIndexToHandlerId(hotbarSlot);

        clickSlot(hotbarId, SlotActionType.PICKUP);
        clickSlot(targetSlot, SlotActionType.PICKUP);
        clickSlot(hotbarId, SlotActionType.PICKUP);

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            int emptySlot = findEmptyHandlerSlot();
            clickSlot(emptySlot != -1 ? emptySlot : targetSlot, SlotActionType.PICKUP);
        }

        return hotbarSlot;
    }

    private void restoreInventory() {
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            int emptySlot = findEmptyHandlerSlot();
            if (emptySlot != -1) {
                clickSlot(emptySlot, SlotActionType.PICKUP);
            }
        }
    }

    private int findEmptyHandlerSlot() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return invIndexToHandlerId(i);
            }
        }
        return -1;
    }

    private int findEmptyHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private static int invIndexToHandlerId(int index) {
        if (index >= 0 && index < 9) return 36 + index;
        if (index >= 9 && index < 36) return index;
        if (index == 40) return 45;
        return -1;
    }

    private void clickSlot(int slot, SlotActionType type) {
        if (mc.player.currentScreenHandler != null) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, type, mc.player);
        }
    }

    private int getNextSequenceId() {
        return (int) (mc.world.getTime() % 1000);
    }

    private Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }

    private BlockHitResult rayCast(double range, float yaw, float pitch) {
        Vec3d vec = getRotationVector(pitch, yaw);
        Vec3d start = mc.player.getEyePos();
        Vec3d end = start.add(vec.multiply(range));
        RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        return mc.world.raycast(ctx);
    }

    @Override
    public String getInfo() {
        return "" + mode.getValue();
    }
}