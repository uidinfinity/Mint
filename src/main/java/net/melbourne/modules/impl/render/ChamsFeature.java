package net.melbourne.modules.impl.render;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;

@FeatureInfo(name = "Chams", category = Category.Render)
public class ChamsFeature extends Feature {

    public BooleanSetting players = new BooleanSetting("Players", "Show players through blocks.", true);
    public BooleanSetting entities = new BooleanSetting("Entities", "Show living entities through blocks.", false);
    public BooleanSetting crystals = new BooleanSetting("Crystals", "Show end crystals through blocks.", true);

    public boolean shouldChams(Entity e) {
        if (!isEnabled()) return false;
        if (e == mc.player && mc.options.getPerspective().isFirstPerson()) return false;

        if (e instanceof EndCrystalEntity) return crystals.getValue();
        if (e instanceof PlayerEntity) return players.getValue();

        return entities.getValue() && (e instanceof LivingEntity);

    }
}
