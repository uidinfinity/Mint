package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.settings.types.WhitelistSetting;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.*;

@FeatureInfo(name = "Trails", category = Category.Render)
public class TrailsFeature extends Feature {

    public final WhitelistSetting targets = new WhitelistSetting("Targets", "Entities to draw trails for", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Pearls", "XP", "Arrows", "Potions"});
    public ModeSetting colorMode = new ModeSetting("Color", "Mode for trail coloring", "Normal", new String[]{"Normal", "Gradient"});
    public ColorSetting mainColor = new ColorSetting("Primary", "Main trail color", new Color(255, 255, 255));
    public ColorSetting secondaryColor = new ColorSetting("Secondary", "Fading trail color", new Color(100, 100, 255), () -> colorMode.getValue().equals("Gradient"));
    public NumberSetting fadeDuration = new NumberSetting("Fade", "Time to fade after landing (ms)", 1000, 0, 5000);

    private final Map<UUID, ProjectileTrail> trailMap = new HashMap<>();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.world == null) {
            trailMap.clear();
            return;
        }

        Set<UUID> activeInWorld = new HashSet<>();
        for (Entity entity : mc.world.getEntities()) {
            if (isValidProjectile(entity)) {
                activeInWorld.add(entity.getUuid());
                trailMap.computeIfAbsent(entity.getUuid(), k -> new ProjectileTrail()).add(entity.getPos());
            }
        }

        long currentTime = System.currentTimeMillis();
        trailMap.entrySet().removeIf(entry -> {
            ProjectileTrail trail = entry.getValue();

            if (!activeInWorld.contains(entry.getKey()) && !trail.dead) {
                trail.dead = true;
                trail.deathTime = currentTime;
            }

            return trail.dead && (currentTime - trail.deathTime) > fadeDuration.getValue().longValue();
        });
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.player == null || trailMap.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        float duration = fadeDuration.getValue().floatValue();

        for (ProjectileTrail trail : trailMap.values()) {
            if (trail.positions.size() < 2) continue;

            float fadeProgress = 1.0f;
            if (trail.dead) {
                float life = (currentTime - trail.deathTime) / duration;
                fadeProgress = 1.0f - Math.min(1.0f, life);
            }

            float alphaMulti = Easing.ease(fadeProgress, Easing.Method.LINEAR);

            for (int i = 0; i < trail.positions.size() - 1; i++) {
                Vec3d start = trail.positions.get(i);
                Vec3d end = trail.positions.get(i + 1);

                Color c1, c2;
                if (colorMode.getValue().equals("Gradient")) {
                    float p1 = (float) i / trail.positions.size();
                    float p2 = (float) (i + 1) / trail.positions.size();
                    c1 = ColorUtils.getColor(interpolate(secondaryColor.getColor(), mainColor.getColor(), p1), (int) (mainColor.getColor().getAlpha() * alphaMulti));
                    c2 = ColorUtils.getColor(interpolate(secondaryColor.getColor(), mainColor.getColor(), p2), (int) (mainColor.getColor().getAlpha() * alphaMulti));
                } else {
                    c1 = ColorUtils.getColor(mainColor.getColor(), (int) (mainColor.getColor().getAlpha() * alphaMulti));
                    c2 = c1;
                }

                Renderer3D.renderLine(event.getContext(), start, end, c1, c2);
            }
        }
    }

    private Color interpolate(Color start, Color end, float progress) {
        return new Color(
                (int) (start.getRed() + (end.getRed() - start.getRed()) * progress),
                (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * progress),
                (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * progress)
        );
    }

    private boolean isValidProjectile(Entity e) {
        if (e instanceof EnderPearlEntity) return targets.getWhitelistIds().contains("Pearls");
        if (e instanceof ExperienceBottleEntity) return targets.getWhitelistIds().contains("XP");
        if (e instanceof ArrowEntity) return targets.getWhitelistIds().contains("Arrows");
        if (e instanceof PotionEntity) return targets.getWhitelistIds().contains("Potions");
        return false;
    }

    @Override
    public void onDisable() {
        trailMap.clear();
    }

    private static class ProjectileTrail {
        List<Vec3d> positions = new ArrayList<>();
        long deathTime = -1;
        boolean dead = false;

        void add(Vec3d pos) {
            if (!dead) positions.add(pos);
        }
    }
}