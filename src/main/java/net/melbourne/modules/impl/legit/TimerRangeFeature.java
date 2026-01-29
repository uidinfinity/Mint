package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.services.Services;

@FeatureInfo(name = "TimerRange", category = Category.Legit)
public class TimerRangeFeature extends Feature {

    public NumberSetting hurtMultiplier = new NumberSetting("Hurt Timer", "Timer multiplier when player is hurt", 0.5f, 0.1f, 2.0f);
    public NumberSetting recoveryMultiplier = new NumberSetting("Recovery Timer", "Timer multiplier while recovering", 2.0f, 0.1f, 5.0f);
    public NumberSetting idleMultiplier = new NumberSetting("Idle Timer", "Timer multiplier while idle", 0.75f, 0.1f, 2.0f);

    private boolean hurt;
    private int ticks;

    @Override
    public void onEnable() {
        hurt = false;
        ticks = 0;
        Services.WORLD.setTimerMultiplier(1.0f);
    }

    @Override
    public void onDisable() {
        hurt = false;
        ticks = 0;
        Services.WORLD.setTimerMultiplier(1.0f);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        if (mc.player.hurtTime > 8) {
            Services.WORLD.setTimerMultiplier(hurtMultiplier.getValue().floatValue());
            hurt = true;
        } else if (mc.player.hurtTime <= 4) {
            if (hurt) {
                Services.WORLD.setTimerMultiplier(recoveryMultiplier.getValue().floatValue());
                if (ticks >= 6) {
                    ticks = 0;
                    hurt = false;
                } else {
                    ticks++;
                }
            } else {
                Services.WORLD.setTimerMultiplier(1.0f);
            }
        } else {
            Services.WORLD.setTimerMultiplier(idleMultiplier.getValue().floatValue());
        }
    }

    @Override
    public String getInfo() {
        return String.format("%.2f", Services.WORLD.getTimerMultiplier());
    }
}
