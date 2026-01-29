package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerDeathEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ColorSetting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;

import java.awt.*;

@FeatureInfo(name = "KillEffects", category = Category.Render)
public class KillEffectsFeature extends Feature {
    public ColorSetting color = new ColorSetting("Color", "The color of the lightning bolt.", new Color(219, 127, 255));

    @SubscribeEvent
    public void onPlayerDeath(PlayerDeathEvent event) {
        LightningEntity entity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);

        entity.setPosition(event.getPlayer().getPos());
        entity.setId(-701);

        mc.world.addEntity(entity);
    }
}
