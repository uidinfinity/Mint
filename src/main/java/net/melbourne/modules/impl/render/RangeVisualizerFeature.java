package net.melbourne.modules.impl.render;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.combat.HoleFillFeature;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.block.hole.HoleUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@FeatureInfo(name = "RangeVisualizer", category = Category.Render)
public class RangeVisualizerFeature extends Feature {

    public final NumberSetting range = new NumberSetting("Range", "Target detection range", 10.0, 1.0, 20.0);
    public final NumberSetting animationTime = new NumberSetting("Animation", "Animation duration", 400, 1, 2000);
    public final ModeSetting shape = new ModeSetting("Shape", "Render style", "Both", new String[]{"Outline", "Fill", "Both"});

    public final ColorSetting lineColor = new ColorSetting("Outline", "Outline color", new Color(150, 0, 255, 255),
            () -> shape.getValue().equals("Outline") || shape.getValue().equals("Both"));
    public final ColorSetting fillColor = new ColorSetting("Fill", "Inside color", new Color(150, 0, 255, 100),
            () -> shape.getValue().equals("Fill") || shape.getValue().equals("Both"));

    public final BooleanSetting exposed = new BooleanSetting("Exposed", "Highlight exposed targets", true);
    public final ColorSetting exposedColor = new ColorSetting("ExposedColor", "Exposed color", new Color(255, 0, 0, 255),
            () -> exposed.getValue());

    public final BooleanSetting holeFillDependent = new BooleanSetting("Advanced", "Sync circle with HoleFill range", true);
    public final NumberSetting size = new NumberSetting("Size", "Circle size multiplier", 1.0, 0.1, 2.0,
            () -> !holeFillDependent.getValue());

    private Animation radiusAnim;
    private Animation fadeOutAnim;
    private Vec3d lastPos = null;
    private Vec3d oldPos = null;
    private float oldRadiusStart = 0f;
    private PlayerEntity lastTarget = null;

    public RangeVisualizerFeature() {
        radiusAnim = new Animation(0, 0, animationTime.getValue().intValue(), Easing.Method.EASE_OUT_QUAD);
        fadeOutAnim = new Animation(0, 0, animationTime.getValue().intValue(), Easing.Method.EASE_OUT_QUAD);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull()) return;

        PlayerEntity target = findTarget();
        HoleFillFeature holeFill = Managers.FEATURE.getFeatureFromClass(HoleFillFeature.class);
        if (holeFill == null) return;

        if (target != lastTarget) {
            if (lastTarget != null && lastPos != null) {
                oldPos = lastPos;
                oldRadiusStart = radiusAnim.getCurrent();
                fadeOutAnim = new Animation(oldRadiusStart, oldRadiusStart, animationTime.getValue().intValue(), Easing.Method.EASE_OUT_QUAD);
                fadeOutAnim.setStartTime(System.currentTimeMillis());
            }
            radiusAnim.setPrev(0);
            radiusAnim.setStartTime(System.currentTimeMillis());
            lastTarget = target;
        }

        double baseRange = holeFillDependent.getValue() ? holeFill.xDist.getValue().doubleValue() : holeFill.xDist.getValue().doubleValue() * size.getValue().doubleValue();
        boolean inHole = target != null && (HoleUtils.isPlayerInHole(target) || isInsideBlock(target));
        double targetRadius = baseRange * (inHole ? 0.1 : 1.0);

        if (radiusAnim.getDuration() != animationTime.getValue().intValue()) {
            radiusAnim = new Animation(radiusAnim.getPrev(), radiusAnim.getCurrent(), animationTime.getValue().intValue(), Easing.Method.EASE_OUT_QUAD);
            fadeOutAnim = new Animation(fadeOutAnim.getPrev(), fadeOutAnim.getCurrent(), animationTime.getValue().intValue(), Easing.Method.EASE_OUT_QUAD);
        }

        if (oldPos != null) {
            float fadeOutRadius = fadeOutAnim.get(0f);
            if (fadeOutRadius > 0.001f) {
                float fadeAlpha = fadeOutRadius / oldRadiusStart;
                drawCircle(event, oldPos, fadeOutRadius, fadeAlpha, false);
            } else {
                oldPos = null;
            }
        }

        float goal = (target == null) ? 0f : (float) targetRadius;
        float animatedRadius = radiusAnim.get(goal);

        if (target != null) {
            lastPos = target.getPos();
            boolean isExposed = exposed.getValue() && !inHole && mc.player.distanceTo(target) <= holeFill.placeRange.getValue().doubleValue();
            float fadeInAlpha = Math.min(animatedRadius / (float) targetRadius, 1.0f);
            drawCircle(event, lastPos, animatedRadius, fadeInAlpha, isExposed);
        }
    }

    private void drawCircle(RenderWorldEvent event, Vec3d pos, float radius, float alphaMultiplier, boolean isExposed) {
        Color lB = isExposed ? exposedColor.getColor() : lineColor.getColor();
        Color fB = isExposed ? exposedColor.getColor() : fillColor.getColor();

        int finalFillAlpha = (int) (fillColor.getColor().getAlpha() * alphaMultiplier);
        int finalLineAlpha = (int) (lineColor.getColor().getAlpha() * alphaMultiplier);

        if (shape.getValue().equals("Fill") || shape.getValue().equals("Both"))
            Renderer3D.renderCircle(event.getContext(), pos, radius, new Color(fB.getRed(), fB.getGreen(), fB.getBlue(), finalFillAlpha));

        if (shape.getValue().equals("Outline") || shape.getValue().equals("Both"))
            Renderer3D.renderCircleOutline(event.getContext(), pos, radius, new Color(lB.getRed(), lB.getGreen(), lB.getBlue(), finalLineAlpha));
    }

    private boolean isInsideBlock(PlayerEntity target) {
        for (BlockPos pos : getOccupiedPositions(target)) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isReplaceable()) return true;
        }
        return false;
    }

    private List<BlockPos> getOccupiedPositions(PlayerEntity target) {
        List<BlockPos> positions = new ArrayList<>();
        Box box = target.getBoundingBox();
        for (double x = box.minX; x <= box.maxX; x += (box.maxX - box.minX)) {
            for (double z = box.minZ; z <= box.maxZ; z += (box.maxZ - box.minZ)) {
                BlockPos pos = new BlockPos(MathHelper.floor(x), MathHelper.floor(box.minY), MathHelper.floor(z));
                if (!positions.contains(pos)) positions.add(pos);
            }
        }
        if (positions.isEmpty()) positions.add(target.getBlockPos());
        return positions;
    }

    private PlayerEntity findTarget() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && !p.isDead() && !Managers.FRIEND.isFriend(p.getName().getString()))
                .filter(p -> mc.player.distanceTo(p) <= range.getValue().doubleValue())
                .min((a, b) -> Double.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)))
                .orElse(null);
    }
}