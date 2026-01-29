package net.melbourne.modules.impl.misc;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.*;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.player.MultiTaskFeature;
import net.melbourne.settings.types.*;
import net.melbourne.utils.Globals;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.inventory.SwitchType;
import net.melbourne.utils.miscellaneous.NetworkUtils;
import net.melbourne.utils.rotation.RotationPoint;
import net.melbourne.utils.rotation.RotationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FeatureInfo(name = "PacketMine", category = Category.Misc)
public class PacketMineFeature extends Feature implements Globals {

    public NumberSetting range = new NumberSetting("Range", "Mining range.", 4.0f, 0.1f, 6.0f);
    public NumberSetting progressBreak = new NumberSetting("ProgressBreak", "Break at progress.", 1.0f, 0.7f, 1.0f);
    public ModeSetting autoSwap = new ModeSetting("AutoSwap", "Swap mode.", "Swap", new String[]{"None", "Normal", "Silent", "Swap", "Pickup"});
    public BooleanSetting awaitBreak = new BooleanSetting("AwaitBreak", "Client-side break.", true);
    public BooleanSetting pauseOnUse = new BooleanSetting("PauseOnUse", "Pause mining while using item.", false);
    public BooleanSetting resetOnSwitch = new BooleanSetting("ResetOnSwitch", "Reset on slot change.", false);
    public BooleanSetting doubleBreak = new BooleanSetting("DoubleBreak", "Double break packets.", false);
    public BooleanSetting reBreak = new BooleanSetting("Rebreak", "Re-mine after break.", true);
    public BooleanSetting instant = new BooleanSetting("Instant", "Instant re-mine.", false);
    public NumberSetting instantDelay = new NumberSetting("InstantDelay", "Delay before instant re-mine.", 0, 0, 500);

    public ModeSetting shape = new ModeSetting("Shape", "Render shape.", "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public ModeSetting color = new ModeSetting("Color", "Color logic for mining.", "Gradient", new String[]{"Custom", "Normal", "Gradient"});
    public ModeSetting render = new ModeSetting("Render", "Render animation.", "Grow", new String[]{"Static", "Grow", "Shrink", "Both"});

    public ColorSetting progressColor = new ColorSetting("Fill", "The primary mining color.", new Color(255, 0, 0, 100),
            () -> color.getValue().equalsIgnoreCase("Custom"));

    public ColorSetting renderOutlineColor = new ColorSetting("Outline", "Mining block outline color.", new Color(255, 255, 255),
            () -> color.getValue().equalsIgnoreCase("Custom"));

    public BooleanSetting renderAir = new BooleanSetting("RenderAir", "Render remine position on air.", true);
    public NumberSetting fadeSpeed = new NumberSetting("FadeSpeed", "Fade out speed (ms)", 300, 50, 1000);
    public BooleanSetting easing = new BooleanSetting("Easing", "Use easing for grow animation.", true);

    private final BooleanSetting rotate = new BooleanSetting("Rotate", "Rotate to mine location.", true);

    private final LinkedList<MiningData> miningQueue = new LinkedList<>();
    private final ConcurrentHashMap<BlockPos, FadeEntry> fadingBlocks = new ConcurrentHashMap<>();

    public boolean isFeaturePaused() {
        return isPaused();
    }

    public MiningData getMiningData() {
        return miningQueue.isEmpty() ? null : miningQueue.getFirst();
    }

    public MiningData getPrevMiningData() {
        return miningQueue.size() > 1 ? miningQueue.get(1) : null;
    }

    public boolean isMining(BlockPos pos) {
        for (MiningData data : miningQueue) {
            if (data.getPos().equals(pos)) return true;
        }
        return fadingBlocks.containsKey(pos);
    }

    private boolean isPaused() {
        return pauseOnUse.getValue() && mc.player.isUsingItem() && !Managers.FEATURE.getFeatureFromClass(MultiTaskFeature.class).isEnabled();
    }

    @Override
    public void onEnable() {
        miningQueue.clear();
        fadingBlocks.clear();
    }

    @Override
    public String getInfo() {
        MiningData miningData = getMiningData();
        if (miningData == null) return "0.0";
        float prog = miningData.getBlockDamage();
        float target = progressBreak.getValue().floatValue();
        float percent = Math.min(prog / target, 1f);
        return String.format("%.1f", percent);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof BlockUpdateS2CPacket packet)) return;
        BlockPos pos = packet.getPos();

        if (packet.getState().isAir()) {
            for (MiningData data : miningQueue) {
                if (pos.equals(data.getPos())) {
                    if (!fadingBlocks.containsKey(pos)) startFade(data);
                    Services.INVENTORY.syncItem();
                    data.onBecameAir();
                    break;
                }
            }
        } else {
            for (MiningData data : miningQueue) {
                if (pos.equals(data.getPos())) {
                    data.onBecameSolid(this);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (resetOnSwitch.getValue() && event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            MiningData miningData = getMiningData();
            if (miningData != null) {
                attemptMine(miningData);
                resetProgress(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull() || mc.player.isCreative()) return;
        if (isPaused()) return;

        if (mc.options.attackKey.isPressed()) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                Direction dir = blockHit.getSide();
                if (canMinePos(pos)) startMiningPos(pos, dir);
            }
        }

        Iterator<MiningData> iterator = miningQueue.iterator();
        while (iterator.hasNext()) {
            MiningData data = iterator.next();

            double dist = mc.player.getEyePos().squaredDistanceTo(data.getPos().toCenterPos());
            if (dist > MathHelper.square(range.getValue().floatValue())) {
                startFade(data);
                iterator.remove();
                continue;
            }

            if (data.process(this)) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (shape.getValue().equalsIgnoreCase("None")) return;

        long now = System.currentTimeMillis();
        long fadeDuration = fadeSpeed.getValue().longValue();

        for (MiningData data : miningQueue) {
            BlockPos pos = data.getPos();
            if (!fadingBlocks.containsKey(pos)) {
                if (!data.getState().isAir() || renderAir.getValue()) {
                    float progress = data.isRemine() ? 1.0f : Math.min(data.getBlockDamage() / progressBreak.getValue().floatValue(), 1.0f);
                    Color fill = getDynamicColor(progress, true);
                    Color outline = getDynamicColor(progress, false);
                    renderAnimatedBox(event, pos, progress, fill, outline, 1.0f);
                }
            }
        }

        Iterator<Map.Entry<BlockPos, FadeEntry>> it = fadingBlocks.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            FadeEntry fade = entry.getValue();
            BlockPos pos = entry.getKey();

            long age = now - fade.startTime;
            if (age > fadeDuration) {
                it.remove();
                continue;
            }

            float fadeProgress = 1.0f - (age / (float) fadeDuration);
            float baseProgress = fade.progressAtFade;
            Color fill = getDynamicColor(baseProgress, true);
            Color outline = getDynamicColor(baseProgress, false);

            renderAnimatedBox(event, pos, baseProgress, fill, outline, fadeProgress);
        }
    }

    private Color getDynamicColor(float progress, boolean isFill) {
        if (color.getValue().equalsIgnoreCase("Custom")) {
            return isFill ? progressColor.getColor() : renderOutlineColor.getColor();
        }

        int alpha = isFill ? progressColor.getColor().getAlpha() : renderOutlineColor.getColor().getAlpha();

        switch (color.getValue()) {
            case "Normal":
                if (progress >= 0.90f) return new Color(0, 255, 0, alpha);
                return new Color(255, 0, 0, alpha);
            case "Gradient":
                return new Color((int) (255 * (1.0f - progress)), (int) (255 * progress), 0, alpha);
            default:
                return isFill ? progressColor.getColor() : renderOutlineColor.getColor();
        }
    }

    private float getRenderT(float progress) {
        float p = MathHelper.clamp(progress, 0f, 1f);
        if (!easing.getValue()) return p;
        return Easing.ease(p, Easing.Method.EASE_OUT_QUAD);
    }

    private double getScaleForMode(float progress) {
        String mode = render.getValue();
        if (mode.equalsIgnoreCase("Static")) return 1.0;

        float t = getRenderT(progress);
        double minScale = 0.01;

        if (mode.equalsIgnoreCase("Grow")) return minScale + (0.99 * t);
        if (mode.equalsIgnoreCase("Shrink")) return minScale + (0.99 * (1.0 - t));
        if (mode.equalsIgnoreCase("Both")) {
            float tri = (t < 0.5f) ? (1.0f - (t * 2.0f)) : ((t - 0.5f) * 2.0f);
            if (easing.getValue()) tri = Easing.ease(tri, Easing.Method.EASE_OUT_QUAD);
            return minScale + (0.99 * tri);
        }

        return 1.0;
    }

    private void renderAnimatedBox(RenderWorldEvent event, BlockPos pos, float progress, Color baseFill, Color baseOutline, float alphaMul) {
        double scale = getScaleForMode(progress);
        if (scale <= 0.011) return;

        double offset = 0.5 * (1.0 - scale);
        Box box = new Box(
                pos.getX() + offset, pos.getY() + offset, pos.getZ() + offset,
                pos.getX() + 1 - offset, pos.getY() + 1 - offset, pos.getZ() + 1 - offset
        );

        int fillA = Math.max(0, (int) (baseFill.getAlpha() * alphaMul));
        int outA = Math.max(0, (int) (baseOutline.getAlpha() * alphaMul));

        Color fill = new Color(baseFill.getRed(), baseFill.getGreen(), baseFill.getBlue(), fillA);
        Color outline = new Color(baseOutline.getRed(), baseOutline.getGreen(), baseOutline.getBlue(), outA);

        if (shape.getValue().equalsIgnoreCase("Fill") || shape.getValue().equalsIgnoreCase("Both"))
            Renderer3D.renderBox(event.getContext(), box, fill);
        if (shape.getValue().equalsIgnoreCase("Outline") || shape.getValue().equalsIgnoreCase("Both"))
            Renderer3D.renderBoxOutline(event.getContext(), box, outline);
    }

    private void startFade(MiningData data) {
        if (data == null) return;
        BlockPos pos = data.getPos();
        float progress = data.isRemine() ? 1.0f : Math.min(data.getBlockDamage() / progressBreak.getValue().floatValue(), 1.0f);
        if (!renderAir.getValue() && mc.world.getBlockState(pos).isAir()) progress = 0.0f;
        fadingBlocks.put(pos, new FadeEntry(System.currentTimeMillis(), progress, data.getBlockDamage() > 0));
    }

    private void attemptMine(MiningData data) {
        if (isPaused()) return;

        Services.INVENTORY.switchTo(data.getToolSlot(), getSwitchType());

        if (rotate.getValue()) {
            Vec3d hitVec = Vec3d.ofCenter(data.getPos()).add(Vec3d.of(data.getDirection().getVector()).multiply(0.5));
            float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), hitVec);
            Services.ROTATION.setRotationPoint(new RotationPoint(rotations[0], rotations[1], 100, true));
        }

        if (doubleBreak.getValue()) {
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection(), s));
        } else {
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection(), s));
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection(), s));
        }

        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (awaitBreak.getValue() && canAwaitBreak(data)) {
            mc.interactionManager.breakBlock(data.getPos());
        }

        Services.INVENTORY.switchBack();
    }

    private boolean canAwaitBreak(MiningData data) {
        BlockState state = mc.world.getBlockState(data.getPos());
        return state.getHardness(mc.world, data.getPos()) <= data.getBreakUtil().getDigSpeed(state) || (data.isRemine() && data.getBlockDamage() >= 1.0f);
    }

    public boolean canMinePos(BlockPos pos) {
        for (MiningData data : miningQueue) {
            if (data.getPos().equals(pos)) return false;
        }
        return canBreak(pos) && !mc.player.getAbilities().creativeMode;
    }

    private boolean canBreak(BlockPos pos) {
        if (getNull()) return false;
        BlockState state = mc.world.getBlockState(pos);
        return state.getHardness(mc.world, pos) != -1.0f;
    }

    private void resetProgress(boolean removeFirst) {
        if (removeFirst && !miningQueue.isEmpty()) {
            MiningData first = miningQueue.removeFirst();
            startFade(first);
        } else {
            miningQueue.clear();
        }
    }

    public void startMiningPos(BlockPos pos, Direction dir) {
        startMiningPos(pos, dir, false);
    }

    public void startMiningPos(BlockPos pos, Direction dir, boolean reset) {
        if (isPaused() || pos == null || dir == null) return;
        if (!canMinePos(pos) || (!reset && mc.world.isAir(pos))) return;

        MiningData newData = new MiningData(pos, dir, progressBreak.getValue().floatValue(), this);

        if (!miningQueue.isEmpty()) {
            if (doubleBreak.getValue() && miningQueue.size() < 2) {
                miningQueue.add(newData);
            } else {
                MiningData current = miningQueue.removeFirst();
                startFade(current);
                miningQueue.addFirst(newData);
            }
        } else {
            miningQueue.add(newData);
        }

        sendStartPacket(pos, dir);
    }

    private void sendStartPacket(BlockPos pos, Direction dir) {
        if (doubleBreak.getValue()) {
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir, s));
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, dir, s));
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir, s));
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir, s));
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, dir, s));
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, dir, s));
        } else {
            NetworkUtils.sendSequencedPacket(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, dir, s));
        }
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private SwitchType getSwitchType() {
        return switch (autoSwap.getValue()) {
            case "None" -> SwitchType.None;
            case "Normal" -> SwitchType.Normal;
            case "Silent" -> SwitchType.Silent;
            case "Swap" -> SwitchType.Swap;
            case "Pickup" -> SwitchType.PickUp;
            default -> SwitchType.Swap;
        };
    }

    public static class MiningData {
        private final BlockPos pos;
        private final Direction direction;
        private final BreakUtil breakUtil;

        private float blockDamage;
        private boolean remine;
        private final float targetProgress;
        private boolean mining;

        private boolean sawAir;
        private long unlockAtMs;

        public MiningData(BlockPos pos, Direction direction, float targetProgress, PacketMineFeature parent) {
            this.pos = pos;
            this.direction = direction;
            this.targetProgress = targetProgress;
            this.breakUtil = new BreakUtil(parent);
            breakUtil.setCurrentState(mc.world.getBlockState(pos));
            this.remine = false;
            this.mining = false;
            this.sawAir = false;
            this.unlockAtMs = 0L;
        }

        public void onBecameAir() {
            sawAir = true;
            mining = false;
            blockDamage = 0.0f;
        }

        public void onBecameSolid(PacketMineFeature parent) {
            if (!parent.reBreak.getValue() || !parent.instant.getValue()) return;
            if (!sawAir) return;
            sawAir = false;
            remine = true;
            mining = false;
            blockDamage = 0.0f;
            long d = parent.instantDelay.getValue().longValue();
            unlockAtMs = System.currentTimeMillis() + Math.max(0L, d);
        }

        public boolean process(PacketMineFeature parent) {
            BlockState state = mc.world.getBlockState(pos);

            if (state.isReplaceable()) {
                if (!parent.reBreak.getValue()) {
                    parent.startFade(this);
                    return true;
                }
                onBecameAir();
                return false;
            }

            if (state.getBlock() != breakUtil.getCurrentState().getBlock()) {
                breakUtil.setCurrentState(state);
            }

            long now = System.currentTimeMillis();
            boolean instantCycle = parent.reBreak.getValue() && parent.instant.getValue() && remine;

            if (instantCycle) {
                if (now < unlockAtMs) {
                    if (!mining) {
                        parent.sendStartPacket(pos, direction);
                        mining = true;
                    }
                    return false;
                }
                blockDamage = 1.0f;
                parent.attemptMine(this);
                mining = false;
                blockDamage = 0.0f;
                remine = true;
                return false;
            }

            if (!mining) {
                parent.sendStartPacket(pos, direction);
                mining = true;
            }

            float delta = breakUtil.calculateBlockStrength(this);
            blockDamage = MathHelper.clamp(blockDamage + delta, 0f, 1.0f);

            if (blockDamage >= targetProgress && !state.isReplaceable()) {
                parent.attemptMine(this);
                if (!parent.reBreak.getValue()) {
                    return true;
                }
                mining = false;
                blockDamage = 0.0f;
                remine = false;
            }

            return false;
        }

        public BlockState getState() {
            return breakUtil.getCurrentState() != null ? breakUtil.getCurrentState() : mc.world.getBlockState(pos);
        }

        public boolean isReady() { return blockDamage >= 1.0f; }
        public int getToolSlot() { return breakUtil.getToolSlot(); }
        public BlockPos getPos() { return pos; }
        public Direction getDirection() { return direction; }
        public float getBlockDamage() { return blockDamage; }
        public boolean isRemine() { return remine; }
        public BreakUtil getBreakUtil() { return breakUtil; }
    }

    public static class BreakUtil implements Globals {
        private BlockState currentState;
        private final PacketMineFeature parent;

        public BreakUtil(PacketMineFeature parent) { this.parent = parent; }
        public void setCurrentState(BlockState currentState) { this.currentState = currentState; }
        public BlockState getCurrentState() { return currentState; }

        public int getToolSlot() {
            if (currentState == null || currentState.isAir()) return -1;
            int index = -1;
            int inventorySlot = Services.INVENTORY.isInventorySwitch(parent.getSwitchType()) ? 36 : 9;
            float best = 1f;
            for (int i = 0; i < inventorySlot; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) continue;
                float digSpeed = EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), stack);
                float destroySpeed = stack.getMiningSpeedMultiplier(currentState);
                if (digSpeed + destroySpeed > best) {
                    best = digSpeed + destroySpeed;
                    index = i;
                }
            }
            return index;
        }

        public float calculateBlockStrength(MiningData data) {
            BlockState state = data.getState();
            float hardness = state.getHardness(mc.world, data.getPos());
            if (hardness <= 0) return 0.0f;

            float offset = (!state.isToolRequired() || (getToolSlot() != -1 && mc.player.getInventory().getStack(getToolSlot()).isSuitableFor(state))) ? 30f : 100f;
            return getDigSpeed(state) / hardness / offset;
        }

        public float getDigSpeed(BlockState state) {
            ItemStack stack = getToolSlot() != -1 ? mc.player.getInventory().getStack(getToolSlot()) : ItemStack.EMPTY;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > 1) {
                int eff = EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), stack);
                if (eff > 0) speed += (float) (Math.pow(eff, 2) + 1);
            }
            if (StatusEffectUtil.hasHaste(mc.player)) speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2f;
            if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) speed *= 0.3f;
            speed *= (float) mc.player.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
            if (!mc.player.isOnGround()) speed /= 5;
            return speed;
        }
    }

    private static class FadeEntry {
        final long startTime;
        final float progressAtFade;
        final boolean wasActive;

        FadeEntry(long startTime, float progressAtFade, boolean wasActive) {
            this.startTime = startTime;
            this.progressAtFade = progressAtFade;
            this.wasActive = wasActive;
        }
    }
}
