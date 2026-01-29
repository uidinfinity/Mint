package net.melbourne.modules.impl.render;

import com.google.common.collect.Streams;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.engine.prediction.PredictionUtil;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@FeatureInfo(name = "Predictions", category = Category.Render)
public class PredictionsFeature extends Feature {

    public BooleanSetting gravity = new BooleanSetting("Gravity", "Applies gravity to the prediction.", true);
    public NumberSetting ticks = new NumberSetting("Ticks", "Ticks ahead to predict.", 5, 1, 20);
    public ModeSetting renderMode = new ModeSetting("Render", "What to render.", "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public ColorSetting fillColor = new ColorSetting("Fill", "Color of the predicted position box fill.", new Color(255, 0, 0, 50));
    public ColorSetting outlineColor = new ColorSetting("Outline", "Color of the predicted position box outline.", new Color(255, 0, 0, 255));

    private final Map<Entity, Vec3d> predictedPositions = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (!isEnabled() || mc.world == null) return;

        int predictionTicks = (int) ticks.getValue();
        float tickDelta = event.getTickDelta();
        boolean gravityEnabled = gravity.getValue();
        String mode = renderMode.getValue();
        Color fill = fillColor.getColor();
        Color outline = outlineColor.getColor();
        boolean renderFill = mode.equals("Fill") || mode.equals("Both");
        boolean renderOutline = mode.equals("Outline") || mode.equals("Both");

        Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof PlayerEntity)
                .forEach(entity -> {
                    if (entity != mc.player && ((LivingEntity) entity).isAlive()) {
                        Vec3d predicted = PredictionUtil.predictPosition(entity, predictionTicks, gravityEnabled);
                        predictedPositions.put(entity, predicted);
                    } else {
                        predictedPositions.remove(entity);
                    }
                });

        if (mode.equals("None")) {
            return;
        }

        for (Map.Entry<Entity, Vec3d> entry : predictedPositions.entrySet()) {
            Entity entity = entry.getKey();
            Vec3d predictedEndPos = entry.getValue();

            if (!entity.isAlive() || entity.isRemoved()) {
                predictedPositions.remove(entity);
                continue;
            }

            double entityWidth = entity.getWidth();
            double entityHeight = entity.getHeight();

            Vec3d boxCenterPos = predictedEndPos;

            double halfWidth = entityWidth / 2.0;

            Box predictedBox = new Box(
                    boxCenterPos.x - halfWidth,
                    boxCenterPos.y,
                    boxCenterPos.z - halfWidth,
                    boxCenterPos.x + halfWidth,
                    boxCenterPos.y + entityHeight,
                    boxCenterPos.z + halfWidth
            );

            if (renderFill && fill.getAlpha() > 0) {
                Renderer3D.renderBox(event.getContext(), predictedBox, fill);
            }

            if (renderOutline && outline.getAlpha() > 0) {
                Renderer3D.renderBoxOutline(event.getContext(), predictedBox, outline);
            }
        }
    }
}