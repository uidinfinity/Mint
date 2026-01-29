package net.melbourne.modules.impl.render;

import com.google.common.collect.Streams;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FeatureInfo(name = "ESP", category = Category.Render)
public class EspFeature extends Feature {
    public ModeSetting boxMode = new ModeSetting("BoxMode", "The style of the box.", "3D", new String[]{"2D", "3D"});
    public ModeSetting renderMode = new ModeSetting("RenderMode", "What to render.", "Both", new String[]{"None", "Fill", "Outline", "Both"});
    public BooleanSetting hurtEffect = new BooleanSetting("HurtEffect", "Changes color to red on hit.", true);
    public BooleanSetting automatic = new BooleanSetting("Automatic", "Uses the team color, instead of global colors.", true);

    public ColorSetting fillColor = new ColorSetting("FillColor", "The color used for the box fill.", new Color(255, 255, 255, 74),
            () -> renderMode.getValue().equals("Fill") || renderMode.getValue().equals("Both"));

    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The color used for outlines.", new Color(255, 255, 255),
            () -> renderMode.getValue().equals("Outline") || renderMode.getValue().equals("Both"));

    private List<Entity> targets = new ArrayList<>();
    private final Map<Entity, Long> hurtTimer = new HashMap<>();
    private final Map<Entity, Long> fadeOutTimer = new HashMap<>();
    private final long HURT_DURATION = 150;
    private final long FADE_OUT_DURATION = 1000;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        this.targets = Streams.stream(mc.world.getEntities())
                .filter(entity -> entity != mc.player && entity instanceof PlayerEntity && ((LivingEntity) entity).isAlive())
                .peek(entity -> {
                    if (entity instanceof LivingEntity living && living.hurtTime > 0) {
                        hurtTimer.put(entity, now + HURT_DURATION);
                    }
                })
                .toList();

        Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof PlayerEntity && !entity.isAlive())
                .forEach(entity -> {
                    if (!fadeOutTimer.containsKey(entity) && !targets.contains(entity)) {
                        fadeOutTimer.put(entity, now);
                    }
                });

        hurtTimer.entrySet().removeIf(e -> {
            for (Entity ent : mc.world.getEntities()) {
                if (ent == e.getKey()) return false;
            }
            return true;
        });

        fadeOutTimer.entrySet().removeIf(e -> {
            boolean inWorld = false;
            for (Entity ent : mc.world.getEntities()) {
                if (ent == e.getKey()) {
                    inWorld = true;
                    break;
                }
            }
            return !inWorld || (now - e.getValue() >= FADE_OUT_DURATION);
        });
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null) return;
        if (targets.isEmpty() && fadeOutTimer.isEmpty()) return;

        long now = System.currentTimeMillis();
        renderEntities(event, targets, now, false);
        renderEntities(event, new ArrayList<>(fadeOutTimer.keySet()), now, true);
    }

    private void renderEntities(RenderWorldEvent event, List<Entity> entities, long now, boolean isFading) {
        for (Entity entity : entities) {
            Vec3d pos = EntityUtils.getRenderPos(entity, event.getTickDelta());

            Box box = new Box(
                    pos.x - entity.getBoundingBox().getLengthX() / 2,
                    pos.y,
                    pos.z - entity.getBoundingBox().getLengthZ() / 2,
                    pos.x + entity.getBoundingBox().getLengthX() / 2,
                    pos.y + entity.getBoundingBox().getLengthY(),
                    pos.z + entity.getBoundingBox().getLengthZ() / 2
            );

            Color baseFill = fillColor.getColor();
            Color baseOutline = outlineColor.getColor();

            if (automatic.getValue() && entity instanceof PlayerEntity player) {
                baseFill = ColorUtils.getColor(new Color(player.getTeamColorValue()), fillColor.getColor().getAlpha());
                baseOutline = ColorUtils.getColor(new Color(player.getTeamColorValue()), outlineColor.getColor().getAlpha());
            }

            Color renderFill = baseFill;
            Color renderOutline = baseOutline;

            if (hurtEffect.getValue() && !isFading) {
                long end = hurtTimer.getOrDefault(entity, 0L);
                if (now < end) {
                    double progress = 1 - ((end - now) / (double) HURT_DURATION);
                    Color redFill = ColorUtils.getColor(Color.RED, baseFill.getAlpha());
                    Color redOutline = ColorUtils.getColor(Color.RED, baseOutline.getAlpha());
                    renderFill = new Color(
                            (int) (redFill.getRed() + (baseFill.getRed() - redFill.getRed()) * progress),
                            (int) (redFill.getGreen() + (baseFill.getGreen() - redFill.getGreen()) * progress),
                            (int) (redFill.getBlue() + (baseFill.getBlue() - redFill.getBlue()) * progress),
                            baseFill.getAlpha()
                    );
                    renderOutline = new Color(
                            (int) (redOutline.getRed() + (baseOutline.getRed() - redOutline.getRed()) * progress),
                            (int) (redOutline.getGreen() + (baseOutline.getGreen() - redOutline.getGreen()) * progress),
                            (int) (redOutline.getBlue() + (baseOutline.getBlue() - redOutline.getBlue()) * progress),
                            baseOutline.getAlpha()
                    );
                }
            }

            if (isFading) {
                long start = fadeOutTimer.getOrDefault(entity, now);
                double progress = (now - start) / (double) FADE_OUT_DURATION;
                int newAlphaFill = (int) (baseFill.getAlpha() * (1 - progress));
                int newAlphaOutline = (int) (baseOutline.getAlpha() * (1 - progress));
                renderFill = new Color(renderFill.getRed(), renderFill.getGreen(), renderFill.getBlue(), Math.max(0, newAlphaFill));
                renderOutline = new Color(renderOutline.getRed(), renderOutline.getGreen(), renderOutline.getBlue(), Math.max(0, newAlphaOutline));
            }

            if (boxMode.getValue().equalsIgnoreCase("3D")) {
                if (renderMode.getValue().equalsIgnoreCase("Fill") || renderMode.getValue().equalsIgnoreCase("Both"))
                    Renderer3D.renderBox(event.getContext(), box, renderFill);
                if (renderMode.getValue().equalsIgnoreCase("Outline") || renderMode.getValue().equalsIgnoreCase("Both"))
                    Renderer3D.renderBoxOutline(event.getContext(), box, renderOutline);
            }
        }
    }

    @SubscribeEvent
    public void onRenderHud(RenderHudEvent event) {
        if (!boxMode.getValue().equalsIgnoreCase("2D")) return;
        if (mc.world == null || (targets.isEmpty() && fadeOutTimer.isEmpty())) return;

        DrawContext context = event.getContext();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        long now = System.currentTimeMillis();
        renderHudEntities(context, tickDelta, targets, now, false);
        renderHudEntities(context, tickDelta, new ArrayList<>(fadeOutTimer.keySet()), now, true);
    }

    private void renderHudEntities(DrawContext context, float tickDelta, List<Entity> entities, long now, boolean isFading) {
        for (Entity entity : entities) {
            Vec3d pos = EntityUtils.getRenderPos(entity, tickDelta);

            Box box = new Box(
                    pos.x - entity.getBoundingBox().getLengthX() / 2,
                    pos.y,
                    pos.z - entity.getBoundingBox().getLengthZ() / 2,
                    pos.x + entity.getBoundingBox().getLengthX() / 2,
                    pos.y + entity.getBoundingBox().getLengthY(),
                    pos.z + entity.getBoundingBox().getLengthZ() / 2
            );

            Vec3d[] corners = {
                    new Vec3d(box.minX, box.minY, box.minZ),
                    new Vec3d(box.maxX, box.minY, box.minZ),
                    new Vec3d(box.maxX, box.minY, box.maxZ),
                    new Vec3d(box.minX, box.minY, box.maxZ),
                    new Vec3d(box.minX, box.maxY, box.minZ),
                    new Vec3d(box.maxX, box.maxY, box.minZ),
                    new Vec3d(box.maxX, box.maxY, box.maxZ),
                    new Vec3d(box.minX, box.maxY, box.maxZ)
            };

            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            boolean visible = false;

            for (Vec3d corner : corners) {
                Vec3d proj = Renderer3D.project(corner);
                if (Renderer3D.projectionVisible(proj)) {
                    visible = true;
                    minX = Math.min(minX, proj.x);
                    maxX = Math.max(maxX, proj.x);
                    minY = Math.min(minY, proj.y);
                    maxY = Math.max(maxY, proj.y);
                }
            }

            if (!visible) continue;

            float left = (float) minX;
            float top = (float) minY;
            float right = (float) maxX;
            float bottom = (float) maxY;

            Color baseFill = fillColor.getColor();
            Color baseOutline = outlineColor.getColor();

            if (automatic.getValue() && entity instanceof PlayerEntity player) {
                baseFill = ColorUtils.getColor(new Color(player.getTeamColorValue()), fillColor.getColor().getAlpha());
                baseOutline = ColorUtils.getColor(new Color(player.getTeamColorValue()), outlineColor.getColor().getAlpha());
            }

            Color renderFill = baseFill;
            Color renderOutline = baseOutline;

            if (hurtEffect.getValue() && !isFading) {
                long end = hurtTimer.getOrDefault(entity, 0L);
                if (now < end) {
                    double progress = 1 - ((end - now) / (double) HURT_DURATION);
                    Color redFill = ColorUtils.getColor(Color.RED, baseFill.getAlpha());
                    Color redOutline = ColorUtils.getColor(Color.RED, baseOutline.getAlpha());
                    renderFill = new Color(
                            (int) (redFill.getRed() + (baseFill.getRed() - redFill.getRed()) * progress),
                            (int) (redFill.getGreen() + (baseFill.getGreen() - redFill.getGreen()) * progress),
                            (int) (redFill.getBlue() + (baseFill.getBlue() - redFill.getBlue()) * progress),
                            baseFill.getAlpha()
                    );
                    renderOutline = new Color(
                            (int) (redOutline.getRed() + (baseOutline.getRed() - redOutline.getRed()) * progress),
                            (int) (redOutline.getGreen() + (baseOutline.getGreen() - redOutline.getGreen()) * progress),
                            (int) (redOutline.getBlue() + (baseOutline.getBlue() - redOutline.getBlue()) * progress),
                            baseOutline.getAlpha()
                    );
                }
            }

            if (isFading) {
                long start = fadeOutTimer.getOrDefault(entity, now);
                double progress = (now - start) / (double) FADE_OUT_DURATION;
                int newAlphaFill = (int) (baseFill.getAlpha() * (1 - progress));
                int newAlphaOutline = (int) (baseOutline.getAlpha() * (1 - progress));
                renderFill = new Color(renderFill.getRed(), renderFill.getGreen(), renderFill.getBlue(), Math.max(0, newAlphaFill));
                renderOutline = new Color(renderOutline.getRed(), renderOutline.getGreen(), renderOutline.getBlue(), Math.max(0, newAlphaOutline));
            }

            if (renderMode.getValue().equalsIgnoreCase("Fill") || renderMode.getValue().equalsIgnoreCase("Both"))
                Renderer2D.renderQuad(context, left, top, right, bottom, renderFill);

            if (renderMode.getValue().equalsIgnoreCase("Outline") || renderMode.getValue().equalsIgnoreCase("Both"))
                Renderer2D.renderOutline(context, left, top, right, bottom, renderOutline);
        }
    }
}