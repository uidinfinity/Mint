package net.melbourne.modules.impl.render;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.RenderEntityEvent;
import net.melbourne.events.impl.RenderHandEvent;
import net.melbourne.events.impl.RenderShaderEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.services.Services;
import net.melbourne.settings.types.*;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

@FeatureInfo(name = "Shaders", category = Category.Render)
public class ShadersFeature extends Feature {
    public ModeSetting shape = new ModeSetting("Shape", "The portions of the effect that will be rendered", "Both", new String[]{"Fill", "Outline", "Both"});
    public ModeSetting mode = new ModeSetting("Mode", "The shader that will be used to render the effects.", "Glow", new String[]{"Solid", "Glow"});

    public NumberSetting glowStrength = new NumberSetting("GlowStrength", "The strength of the glow effect.", 6, 1, 20,
            () -> mode.getValue().equals("Glow"));
    public NumberSetting glowMultiplier = new NumberSetting("GlowMultiplier", "The multiplier that will be added on top of the glow effect.", 1.0f, 0.1f, 3.0f,
            () -> mode.getValue().equals("Glow"));
    public NumberSetting glowQuality = new NumberSetting("GlowQuality", "The quality of the glow effect.", 10, 1, 10,
            () -> mode.getValue().equals("Glow"));

    public ColorSetting color = new ColorSetting("Color", "The color that will be used for the shader effect rendering.", new Color(255, 255, 255));
    public BooleanSetting hurtEffect = new BooleanSetting("HurtEffect", "Renders a different color when entities are hurt.", true);
    public ColorSetting hurtColor = new ColorSetting("HurtColor", "The color used when an entity is hurt.", new Color(255, 0, 0),
            () -> hurtEffect.getValue());

    public ModeSetting distance = new ModeSetting("Distance", "Prevents entities at a certain distance from having the shader rendered on them.", "None", new String[]{"None", "Crystals", "All"});
    public NumberSetting distanceAmount = new NumberSetting("DistanceAmount", "The distance at which the selected entities will stop having the shader rendered on them.", 15.0, 0.0, 150.0,
            () -> !distance.getValue().equals("None"));

    public final WhitelistSetting targets = new WhitelistSetting("Targets", "Entities to render the shader on", WhitelistSetting.Type.CUSTOM, new String[]{}, new String[]{"Players", "Animals", "Monsters", "Ambient", "Items", "Crystals", "Others", "Hands"});

    @SubscribeEvent
    public void onRenderShader(RenderShaderEvent event) {
        Services.SHADER.prepare();
    }

    @SubscribeEvent
    public void onRenderEntity(RenderEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        Entity entity = event.getEntity();
        if (!shouldRender(entity)) return;

        Color renderColor;
        if (hurtEffect.getValue() && entity instanceof LivingEntity living && living.hurtTime > 0) {
            renderColor = ColorUtils.getColor(hurtColor.getColor(), 255);
        } else if (entity instanceof PlayerEntity player && Managers.FRIEND.isFriend(player.getName().getString())) {
            renderColor = Color.CYAN;
        } else {
            renderColor = ColorUtils.getColor(this.color.getColor(), 255);
        }

//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);

        event.setVertexConsumers(Services.SHADER.create(event.getVertexConsumers(), renderColor));
    }

    @SubscribeEvent
    public void onRenderEntity$POST(RenderEntityEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (!targets.getWhitelistIds().isEmpty()) {
            Services.SHADER.getVertexConsumers().draw();
        }
//      if (!BotManager.INSTANCE.isAuthed())
//          System.exit(0);
        if (!targets.getWhitelistIds().contains("Hands")) render();
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (!targets.getWhitelistIds().contains("Hands")) return;
        event.setVertexConsumers(Services.SHADER.create(event.getVertexConsumers(), ColorUtils.getColor(color.getColor(), 255)));
    }

    @SubscribeEvent
    public void onRenderHand$POST(RenderHandEvent.Post event) {
        if (!targets.getWhitelistIds().contains("Hands")) return;
        Services.SHADER.getVertexConsumers().draw();
    }

    @SubscribeEvent
    public void onRenderShader$POST(RenderShaderEvent.Post event) {
        if (!targets.getWhitelistIds().contains("Hands")) return;
        render();
    }

    private void render() {
        if (mode.getValue().equalsIgnoreCase("Glow")) {
            int shapeIdx = this.shape.getValue().equalsIgnoreCase("Fill") ? 0 : 2;
            Services.SHADER.render("Glow", 32, (builder) -> {
                builder.putInt(shapeIdx)
                        .putInt(glowStrength.getValue().intValue())
                        .putFloat(glowMultiplier.getValue().floatValue())
                        .putInt(1 + (10 - glowQuality.getValue().intValue()))
                        .putFloat(this.shape.getValue().equalsIgnoreCase("Outline") ? 0.0f : color.getColor().getAlpha() / 255.0f);
            });
        } else {
            int shapeIdx = switch (this.shape.getValue()) {
                case "Fill" -> 0;
                case "Outline" -> 1;
                default -> 2;
            };
            Services.SHADER.render("Solid", 16, (builder) -> {
                builder.putInt(shapeIdx)
                        .putFloat(color.getColor().getAlpha() / 255.0f);
            });
        }
    }

    private boolean shouldRender(Entity entity) {
        if (distance.getValue().equalsIgnoreCase("All") && mc.player.squaredDistanceTo(entity) > MathHelper.square(distanceAmount.getValue().doubleValue())) return false;

        if (targets.getWhitelistIds().contains("Players") && entity instanceof PlayerEntity) return true;
        if (targets.getWhitelistIds().contains("Animals") && (entity.getType().getSpawnGroup() == SpawnGroup.CREATURE || entity.getType().getSpawnGroup() == SpawnGroup.WATER_CREATURE)) return true;
        if (targets.getWhitelistIds().contains("Monsters") && entity.getType().getSpawnGroup() == SpawnGroup.MONSTER) return true;
        if (targets.getWhitelistIds().contains("Ambient") && (entity.getType().getSpawnGroup() == SpawnGroup.AMBIENT || entity.getType().getSpawnGroup() == SpawnGroup.WATER_AMBIENT)) return true;
        if (targets.getWhitelistIds().contains("Items") && entity instanceof ItemEntity) return true;
        if (targets.getWhitelistIds().contains("Crystals") && entity instanceof EndCrystalEntity) {
            return !distance.getValue().equalsIgnoreCase("Crystals") || !(mc.player.squaredDistanceTo(entity) > MathHelper.square(distanceAmount.getValue().doubleValue()));
        }

        return targets.getWhitelistIds().contains("Others");
    }
}