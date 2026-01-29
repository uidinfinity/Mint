package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.AttackEntityEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;

@FeatureInfo(name = "SprintReset", category = Category.Movement)
public class SprintResetFeature extends Feature {

    public ModeSetting mode = new ModeSetting("Mode", "W-Tap", "W-Tap", new String[]{"W-Tap", "S-Tap", "SprintTap"});
    public NumberSetting chance = new NumberSetting("Chance", "Chance to apply extra knockback.", 85, 0, 100);

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getTarget() == null) return;
        if (chance.getValue().intValue() != 100 && Math.random() * 100 > chance.getValue().doubleValue())
            return;

        boolean forwardWasDown = mc.options.forwardKey.isPressed();

        switch (mode.getValue()) {

            case "W-Tap": {
                mc.options.forwardKey.setPressed(false);

                if (forwardWasDown)
                    mc.options.forwardKey.setPressed(true);
                break;
            }

            case "S-Tap": {
                boolean backWasDown = mc.options.backKey.isPressed();

                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(true);
                mc.options.backKey.setPressed(false);

                if (forwardWasDown)
                    mc.options.forwardKey.setPressed(true);

                if (backWasDown)
                    mc.options.backKey.setPressed(true);
                break;
            }

            case "SprintTap": {
                boolean sprintWasDown = mc.options.sprintKey.isPressed();

                mc.options.sprintKey.setPressed(false);
                mc.options.sprintKey.setPressed(true);

                if (!sprintWasDown)
                    mc.options.sprintKey.setPressed(false);

                break;
            }
        }
    }

    @Override
    public String getInfo() {
        return "" + mode.getValue();
    }
}
