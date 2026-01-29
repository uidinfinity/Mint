package net.melbourne.modules.impl.render;

import net.melbourne.Managers;
import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.client.EngineFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.entity.player.socials.FriendManager;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@FeatureInfo(name = "BreakHighlight", category = Category.Render)
public class BreakHighlightFeature extends Feature {
    private static final long FULL_GROW_MS = 2200L;

    public NumberSetting range = new NumberSetting("Range", "Max range to render mining blocks.", 5.0f, 1.0f, 10.0f);
    public NumberSetting fade = new NumberSetting("Fade", "Time to fade out in ms.", 500, 0, 2000);
    public ModeSetting logicMode = new ModeSetting("Logic", "How mining positions are handled.", "Continuous", new String[]{"Continuous", "Staged"});
    public ModeSetting animationType = new ModeSetting("Animation", "The movement style of the highlight.", "Grow", new String[]{"Grow", "Shrink", "Both", "Static"});
    public ModeSetting shape = new ModeSetting("Shape", "Visual style of the highlight.", "Both", new String[]{"Line", "Fill", "Both"});

    public ColorSetting fillColor = new ColorSetting("Fill", "The color used for the box fill.", new Color(255, 0, 0, 100),
            () -> shape.getValue().equals("Fill") || shape.getValue().equals("Both"));

    public ColorSetting outlineColor = new ColorSetting("Outline", "The color used for outlines.", new Color(255, 255, 255),
            () -> shape.getValue().equals("Line") || shape.getValue().equals("Both"));

    public BooleanSetting self = new BooleanSetting("Self", "Highlights blocks you're breaking.", false);
    public BooleanSetting others = new BooleanSetting("Others", "Highlights blocks other players are breaking.", true);

    private final Map<BlockPos, Animation> animationMap = new HashMap<>();
    private final Map<BlockPos, Long> timePositions = new HashMap<>();
    private final Map<BlockPos, Long> lastUpdateMap = new HashMap<>();
    private final Map<UUID, BlockPos> playerTargets = new HashMap<>();
    private final Map<BlockPos, Long> stagedCooldown = new HashMap<>();
    private final ConcurrentHashMap<BlockPos, FadeBlock> fadingBlocks = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull()) return;
        if (event.getPacket() instanceof BlockBreakingProgressS2CPacket packet) {
            BlockPos pos = packet.getPos();
            long now = System.currentTimeMillis();

            if (packet.getProgress() >= 0 && packet.getProgress() <= 8) {
                if (!timePositions.containsKey(pos) && !stagedCooldown.containsKey(pos)) {
                    timePositions.put(pos, now);
                }
                lastUpdateMap.put(pos, now);
            } else {
                doFade(pos);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull() || mc.worldRenderer == null || mc.player == null) return;

        float maxRangeSq = MathHelper.square(range.getValue().floatValue());
        Map<BlockPos, Animation> activeAnimations = new HashMap<>();
        FriendManager friendManager = Managers.FRIEND;
        long now = System.currentTimeMillis();

        EngineFeature engine = Managers.FEATURE.getFeatureFromClass(EngineFeature.class);
        boolean engineMining = engine != null && engine.predictions.getWhitelistIds().contains("Mining");
        boolean isStaged = logicMode.getValue().equalsIgnoreCase("Staged");

        Map<BlockPos, Boolean> currentActiveBlocks = new HashMap<>();

        timePositions.forEach((pos, startTime) -> {
            if (pos.getSquaredDistance(mc.player.getEyePos()) > maxRangeSq) return;

            long lastUpdate = lastUpdateMap.getOrDefault(pos, 0L);
            if (now - lastUpdate > 1000L && !engineMining) return;

            float progress = getLogicProgress(pos, now, activeAnimations, isStaged);

            if (progress == -2.0f) {
                doFade(pos);
                if (isStaged) stagedCooldown.put(pos, now);
            } else if (progress >= 0) {
                currentActiveBlocks.put(pos, true);
                fadingBlocks.remove(pos);
                render(event, pos, progress, fillColor.getColor(), outlineColor.getColor(), 1.0f);
            } else {
                doFade(pos);
            }
        });

        if (engineMining && others.getValue()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || player.squaredDistanceTo(mc.player) > 36.0) continue;
                if (friendManager.isFriend(player.getName().getString())) continue;

                BlockPos predicted = Services.SIMULATION.findSurroundMineTarget(player);
                BlockPos lastPredicted = playerTargets.get(player.getUuid());

                if (lastPredicted != null && !lastPredicted.equals(predicted)) {
                    doFade(lastPredicted);
                }

                if (predicted != null) {
                    playerTargets.put(player.getUuid(), predicted);
                    if (predicted.getSquaredDistance(mc.player.getEyePos()) <= maxRangeSq) {
                        float progress = getLogicProgress(predicted, now, activeAnimations, isStaged);
                        if (progress == -2.0f) {
                            doFade(predicted);
                            if (isStaged) stagedCooldown.put(predicted, now);
                        } else if (progress >= 0) {
                            currentActiveBlocks.put(predicted, true);
                            fadingBlocks.remove(predicted);
                            render(event, predicted, progress, fillColor.getColor(), outlineColor.getColor(), 1.0f);
                        } else {
                            doFade(predicted);
                        }
                    }
                } else {
                    playerTargets.remove(player.getUuid());
                }
            }
        }

        timePositions.keySet().removeIf(pos -> {
            if (!currentActiveBlocks.containsKey(pos)) {
                doFade(pos);
                return true;
            }
            return false;
        });

        getFadingBlocks(event, now);

        animationMap.keySet().retainAll(activeAnimations.keySet());
        stagedCooldown.entrySet().removeIf(e -> now - e.getValue() > 1500L);
        timePositions.entrySet().removeIf(e -> mc.world.getBlockState(e.getKey()).isAir());
    }

    private float getLogicProgress(BlockPos pos, long now, Map<BlockPos, Animation> active, boolean isStaged) {
        if (isStaged && stagedCooldown.containsKey(pos)) {
            return -1.0f;
        }

        if (!timePositions.containsKey(pos)) {
            timePositions.put(pos, now);
        }

        long elapsed = now - timePositions.get(pos);

        if (elapsed > (FULL_GROW_MS + 1000L)) {
            if (isStaged) {
                return -2.0f;
            } else {
                timePositions.put(pos, now);
                elapsed = 0;
            }
        }

        float progress = MathHelper.clamp((float) elapsed / FULL_GROW_MS, 0.0f, 1.0f);
        Animation anim = animationMap.computeIfAbsent(pos, p -> new Animation(progress, progress, 225, Easing.Method.EASE_OUT_QUAD));
        float animatedProgress = anim.get(progress);
        active.put(pos, anim);
        return animatedProgress;
    }

    private void getFadingBlocks(RenderWorldEvent event, long now) {
        fadingBlocks.forEach((pos, fadeData) -> {
            long age = now - fadeData.startTime;
            float fadeLifetime = fade.getValue().floatValue();
            if (age >= fadeLifetime || (mc.world.getBlockState(pos).isAir() && age > 50)) {
                fadingBlocks.remove(pos);
            } else {
                float fadeAlpha = 1.0f - (age / fadeLifetime);
                render(event, pos, fadeData.lastProgress, fillColor.getColor(), outlineColor.getColor(), fadeAlpha);
            }
        });
    }

    private void doFade(BlockPos pos) {
        if (fadingBlocks.containsKey(pos) || !timePositions.containsKey(pos)) return;

        float lastProgress = 1.0f;
        if (animationMap.containsKey(pos)) {
            lastProgress = animationMap.get(pos).get(0);
        }

        fadingBlocks.put(pos, new FadeBlock(System.currentTimeMillis(), lastProgress));
        timePositions.remove(pos);
        animationMap.remove(pos);
    }

    private void render(RenderWorldEvent event, BlockPos pos, float progress, Color bF, Color bO, float alphaMult) {
        if (progress <= 0.01f) return;
        float eased = Easing.ease(progress, Easing.Method.EASE_OUT_QUAD);
        String animMode = animationType.getValue();
        String shapeMode = shape.getValue();
        Box drawBox = new Box(pos);

        if (animMode.equalsIgnoreCase("Grow") || animMode.equalsIgnoreCase("Both")) {
            double scale = 0.01 + (0.99 * eased);
            double offset = 0.5 * (1.0 - scale);
            drawBox = new Box(pos.getX() + offset, pos.getY() + offset, pos.getZ() + offset,
                    pos.getX() + 1 - offset, pos.getY() + 1 - offset, pos.getZ() + 1 - offset);
        } else if (animMode.equalsIgnoreCase("Shrink")) {
            double finalN = 0.5 - eased * 0.5;
            drawBox = new Box(pos).expand(-finalN, -finalN, -finalN);
        }

        Color finalFill = new Color(bF.getRed(), bF.getGreen(), bF.getBlue(), (int) (bF.getAlpha() * alphaMult));
        Color finalLine = new Color(bO.getRed(), bO.getGreen(), bO.getBlue(), (int) (255 * alphaMult));

        if (shapeMode.equalsIgnoreCase("Fill") || shapeMode.equalsIgnoreCase("Both"))
            Renderer3D.renderBox(event.getContext(), drawBox, finalFill);
        if (shapeMode.equalsIgnoreCase("Line") || shapeMode.equalsIgnoreCase("Both"))
            Renderer3D.renderBoxOutline(event.getContext(), drawBox, finalLine);
    }

    private static class FadeBlock {
        long startTime;
        float lastProgress;

        FadeBlock(long startTime, float lastProgress) {
            this.startTime = startTime;
            this.lastProgress = lastProgress;
        }
    }
}