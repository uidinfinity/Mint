package net.melbourne.modules.impl.render;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.*;
import net.melbourne.utils.entity.player.movement.position.PositionUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FeatureInfo(name = "PhaseHighlight", category = Category.Render)
public class PhaseHighlightFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "Phasing highlight method", "Pearl", new String[]{"Pearl", "Walk"});
    public BooleanSetting requireMovement = new BooleanSetting("RequireMovement", "Only highlight when moving keys are pressed.", true, () -> mode.getValue().equals("Walk"));
    public ModeSetting fill = new ModeSetting("Fill", "The mode for the fill rendering on the phase blocks.", "Normal", new String[]{"None", "Normal"});
    public ModeSetting outline = new ModeSetting("Outline", "The mode for the outline rendering on the phase blocks.", "Normal", new String[]{"None", "Normal"});
    public NumberSetting height = new NumberSetting("Height", "The height of the phase highlight.", 1.0, -2.0, 2.0);
    public NumberSetting animationDuration = new NumberSetting("AnimationDuration", "The duration in milliseconds for the fade in/out animation.", 500, 0, 5000);

    public final WhitelistSetting blocks = new WhitelistSetting("Blocks", "Block bases to highlight", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Bedrock", "Obsidian", "Air"});

    public ColorSetting bedrockFillColor = new ColorSetting("BedrockFillColor", "Bedrock Fill", new Color(0, 255, 0, 40), () -> blocks.getWhitelistIds().contains("Bedrock"));
    public ColorSetting bedrockOutlineColor = new ColorSetting("BedrockOutlineColor", "Bedrock Outline", new Color(0, 255, 0, 120), () -> blocks.getWhitelistIds().contains("Bedrock"));

    public ColorSetting obsidianFillColor = new ColorSetting("ObsidianFillColor", "Obsidian Fill", new Color(255, 255, 0, 40), () -> blocks.getWhitelistIds().contains("Obsidian"));
    public ColorSetting obsidianOutlineColor = new ColorSetting("ObsidianOutlineColor", "Obsidian Outline", new Color(255, 255, 0, 120), () -> blocks.getWhitelistIds().contains("Obsidian"));

    public ColorSetting airFillColor = new ColorSetting("AirFillColor", "Air Fill", new Color(255, 0, 0, 40), () -> blocks.getWhitelistIds().contains("Air"));
    public ColorSetting airOutlineColor = new ColorSetting("AirOutlineColor", "Air Outline", new Color(255, 0, 0, 120), () -> blocks.getWhitelistIds().contains("Air"));

    private enum PhaseType {
        BEDROCK, OBSIDIAN, AIR
    }

    private final Map<BlockPos, PhaseBlock> phaseBlockMap = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        List<BlockPos> foundPositions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        if (mode.getValue().equals("Pearl")) {
            BlockPos playerPos = PositionUtils.getFlooredPosition(mc.player);
            for (Direction direction : Direction.values()) {
                if (!direction.getAxis().isHorizontal()) continue;
                BlockPos offsetPos = playerPos.offset(direction);
                if (mc.world.getBlockState(offsetPos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(offsetPos).getBlock() != Blocks.OBSIDIAN) continue;

                PhaseType type = getPhaseType(offsetPos);
                if (type != null) {
                    foundPositions.add(offsetPos);
                    phaseBlockMap.computeIfAbsent(offsetPos, k -> new PhaseBlock(new Box(offsetPos), type, currentTime));
                }
            }
        } else if (mode.getValue().equals("Walk")) {
            if (isInsideBlock()) {
                double yawRad = Math.toRadians(mc.player.getYaw());
                boolean isForward = mc.options.forwardKey.isPressed();
                boolean isBack = mc.options.backKey.isPressed();
                boolean isLeft = mc.options.leftKey.isPressed();
                boolean isRight = mc.options.rightKey.isPressed();

                double forward = isForward ? 1 : (isBack ? -1 : 0);
                double strafe = isLeft ? 1 : (isRight ? -1 : 0);

                if (!requireMovement.getValue() || (forward != 0 || strafe != 0)) {
                    double xDir, zDir;

                    if (forward == 0 && strafe == 0) {
                        xDir = -Math.sin(yawRad);
                        zDir = Math.cos(yawRad);
                    } else {
                        xDir = (forward * -Math.sin(yawRad) + strafe * Math.cos(yawRad));
                        zDir = (forward * Math.cos(yawRad) - strafe * -Math.sin(yawRad));
                    }

                    BlockPos predictPos = new BlockPos(MathHelper.floor(mc.player.getX() + xDir), MathHelper.floor(mc.player.getY()), MathHelper.floor(mc.player.getZ() + zDir));
                    PhaseType type = getPhaseTypeFromBlock(predictPos);

                    if (type != null) {
                        foundPositions.add(predictPos);
                        phaseBlockMap.computeIfAbsent(predictPos, k -> new PhaseBlock(new Box(predictPos), type, currentTime));
                    }
                }
            }
        }

        long duration = animationDuration.getValue().longValue();
        phaseBlockMap.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            PhaseBlock block = entry.getValue();

            if (!foundPositions.contains(pos)) {
                if (block.removeTime() == -1) {
                    phaseBlockMap.put(pos, block.withRemoveTime(currentTime));
                    return false;
                }
                return currentTime - block.removeTime() > duration;
            }
            return false;
        });
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null || phaseBlockMap.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        long duration = animationDuration.getValue().longValue();

        for (PhaseBlock phaseBlock : phaseBlockMap.values()) {
            double minY = phaseBlock.box().minY;
            double renderHeight = height.getValue().doubleValue();

            Box box = new Box(phaseBlock.box().minX, minY, phaseBlock.box().minZ,
                    phaseBlock.box().maxX, minY + renderHeight, phaseBlock.box().maxZ);

            float alpha = getAlpha(phaseBlock, currentTime, duration);

            Color baseFillColor = getFillColor(phaseBlock.type());
            Color baseOutlineColor = getOutlineColor(phaseBlock.type());

            Color fillColor = ColorUtils.getColor(baseFillColor, (int) (baseFillColor.getAlpha() * alpha));
            Color outlineColor = ColorUtils.getColor(baseOutlineColor, (int) (baseOutlineColor.getAlpha() * alpha));

            if (fill.getValue().equalsIgnoreCase("Normal")) {
                Renderer3D.renderBox(event.getContext(), box, fillColor, fillColor);
            }

            if (outline.getValue().equalsIgnoreCase("Normal")) {
                Renderer3D.renderBoxOutline(event.getContext(), box, outlineColor, outlineColor);
            }
        }
    }

    private boolean isInsideBlock() {
        Box box = mc.player.getBoundingBox().contract(0.0625);
        for (BlockPos pos : BlockPos.iterate(
                MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ),
                MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ)
        )) {
            if (!mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private float getAlpha(PhaseBlock block, long currentTime, long duration) {
        if (duration == 0) return 1.0f;
        float alpha;
        if (block.removeTime() != -1) {
            float fadeOutTime = currentTime - block.removeTime();
            alpha = 1.0f - Math.min(1.0f, fadeOutTime / duration);
        } else {
            float fadeInTime = currentTime - block.createTime();
            alpha = Math.min(1.0f, fadeInTime / duration);
        }
        return alpha;
    }

    private Color getFillColor(PhaseType type) {
        return switch (type) {
            case BEDROCK -> bedrockFillColor.getColor();
            case OBSIDIAN -> obsidianFillColor.getColor();
            default -> airFillColor.getColor();
        };
    }

    private Color getOutlineColor(PhaseType type) {
        return switch (type) {
            case BEDROCK -> bedrockOutlineColor.getColor();
            case OBSIDIAN -> obsidianOutlineColor.getColor();
            default -> airOutlineColor.getColor();
        };
    }

    private PhaseType getPhaseType(BlockPos pos) {
        if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK && blocks.getWhitelistIds().contains("Bedrock")) return PhaseType.BEDROCK;
        if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN && blocks.getWhitelistIds().contains("Obsidian")) return PhaseType.OBSIDIAN;
        if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.AIR && blocks.getWhitelistIds().contains("Air")) return PhaseType.AIR;
        return null;
    }

    private PhaseType getPhaseTypeFromBlock(BlockPos pos) {
        if (mc.world.getBlockState(pos).isOf(Blocks.BEDROCK) && blocks.getWhitelistIds().contains("Bedrock")) return PhaseType.BEDROCK;
        if (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) && blocks.getWhitelistIds().contains("Obsidian")) return PhaseType.OBSIDIAN;
        if (mc.world.getBlockState(pos).isAir() && blocks.getWhitelistIds().contains("Air")) return PhaseType.AIR;
        return null;
    }

    public record PhaseBlock(Box box, PhaseType type, long createTime, long removeTime) {
        public PhaseBlock(Box box, PhaseType type, long createTime) {
            this(box, type, createTime, -1);
        }

        public PhaseBlock withRemoveTime(long removeTime) {
            return new PhaseBlock(box, type, createTime, removeTime);
        }
    }
}