package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@FeatureInfo(name = "AutoGoldenHead", category = Category.Legit)
public class AutoGoldenHeadFeature extends Feature {

    public final NumberSetting minHealth = new NumberSetting("MinHealth", "Eat head under this HP", 12, 1, 20);
    public final NumberSetting cooldown = new NumberSetting("Cooldown", "Cooldown before next head use (ms)", 500, 0, 5000);
    public final NumberSetting swapDelay = new NumberSetting("SwapDelay", "Delay before swapping back (ms)", 150, 0, 1000);

    private long lastUsed = 0L;
    private long swapTime = 0L;
    private int oldSlot = -1;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        long now = System.currentTimeMillis();

        if (oldSlot != -1 && now >= swapTime) {
            mc.player.getInventory().setSelectedSlot(oldSlot);
            oldSlot = -1;
            return;
        }

        if (mc.player.getHealth() > minHealth.getValue().doubleValue()) return;
        if (now - lastUsed < cooldown.getValue().longValue()) return;

        int headSlot = findGoldenHead();
        if (headSlot == -1) return;

        oldSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(headSlot);

        try {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } catch (Exception ignored) {}

        lastUsed = now;
        swapTime = now + swapDelay.getValue().longValue();
    }

    private int findGoldenHead() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack == null) continue;
            if (stack.getItem() == Items.PLAYER_HEAD) return i;
        }
        return -1;
    }
}
