package net.melbourne.modules.impl.render;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.passive.RabbitEntity;

@FeatureInfo(name = "Freelook", category = Category.Render)
public class FreelookFeature extends Feature {
    public NumberSetting speed = new NumberSetting("Speed", "Changes speed for Freelook.", 5f, .1f, 10f);
    public float yaw, pitch;

    @Override
    public void onDisable() {
        if (getNull())
            return;

        mc.options.setPerspective(Perspective.FIRST_PERSON);
    }

    @Override
    public void onEnable() {
        if (getNull())
            return;

        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();

        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }
}
