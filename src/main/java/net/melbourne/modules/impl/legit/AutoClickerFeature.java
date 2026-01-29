package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.MouseEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

@FeatureInfo(name = "AutoClicker", category = Category.Legit)
public class AutoClickerFeature extends Feature {

    public static volatile boolean holding = false;

    public final NumberSetting cps = new NumberSetting("CPS", "Base clicks per second", 12, 1, 30);
    public final NumberSetting randomization = new NumberSetting("Randomization", "Max CPS jitter (± this value)\n0 = no randomization", 4, 0, 15);

    private long click = 0;
    private final Random random = new Random();

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (mc.currentScreen != null) return;
        if (event.getButton() != 0) return;
        holding = mc.options.attackKey.isPressed();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!holding || getNull() || mc.currentScreen != null) {
            click = 0;
            return;
        }

        if (mc.options.useKey.isPressed()) return;

        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) return;

        long now = System.currentTimeMillis();
        if (now < click) return;

        mc.options.attackKey.setPressed(true);

        double baseCps = (double) cps.getValue();
        double randAmount = (double) randomization.getValue();

        double finalCps = baseCps;
        if (randAmount > 0) {
            finalCps += (random.nextDouble() * 2 - 1) * randAmount;
        }

        finalCps = Math.max(1, Math.min(50, finalCps));

        long delay = (long) (1000.0 / finalCps);
        delay += random.nextInt(15) - 7;

        click = now + Math.max(10, delay);
    }

    @Override
    public void onEnable() {
        holding = false;
        click = 0;
    }

    @Override
    public void onDisable() {
        holding = false;
        click = 0;
    }

    public boolean getNull() {
        return mc.player == null || mc.world == null;
    }
}