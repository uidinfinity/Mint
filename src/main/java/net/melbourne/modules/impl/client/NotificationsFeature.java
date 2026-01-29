package net.melbourne.modules.impl.client;

import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerDeathEvent;
import net.melbourne.events.impl.PlayerPopEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.text.Text;

@FeatureInfo(name = "Notifications", category = Category.Client)
public class NotificationsFeature extends Feature {

    public final BooleanSetting totemPops = new BooleanSetting("TotemPops", "Show totem pop and death notifications.", true);
    public final BooleanSetting moduleToggles = new BooleanSetting("ModuleToggles", "Show module enable/disable notifications.", true);
    public final BooleanSetting pingSpike = new BooleanSetting("LatencySpike", "Detects sudden latency increases.", true);
    public final NumberSetting spikeThreshold = new NumberSetting("LatencyThreshold", "Ping increase required to trigger.", 100, 1, 500, () -> pingSpike.getValue());

    @SubscribeEvent
    public void onPlayerPop(PlayerPopEvent event) {
        if (getNull() || !totemPops.getValue()) return;

        boolean isSelf = event.getPlayer() == mc.player;
        String name = isSelf ? "You" : event.getPlayer().getName().getString();
        String amount = String.valueOf(event.getPops());
        String totems = event.getPops() == 1 ? "totem" : "totems";

        Text nameText = Text.literal(name).styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB()));
        Text amountText = Text.literal(amount).styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB()));

        Text msg = Text.literal("")
                .append(nameText)
                .append(Text.literal(" §7has popped §s"))
                .append(amountText)
                .append(Text.literal(" §7" + totems + "§s"))
                .append(Text.literal("§7."));

        Services.CHAT.sendPop(event.getPlayer().getUuid(), msg, true);
    }

    @SubscribeEvent
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (getNull() || !totemPops.getValue()) return;

        int pops = Services.WORLD.getPoppedTotems().getOrDefault(event.getPlayer().getUuid(), 0);
        if (pops <= 0) return;

        boolean isSelf = event.getPlayer() == mc.player;
        String name = isSelf ? "You" : event.getPlayer().getName().getString();
        String amount = String.valueOf(pops);
        String totems = pops == 1 ? "totem" : "totems";

        Text nameText = Text.literal(name).styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB()));
        Text amountText = Text.literal(amount).styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB()));

        Text msg = Text.literal("")
                .append(nameText)
                .append(Text.literal(" §7has died after popping §s"))
                .append(amountText)
                .append(Text.literal(" §7" + totems + "§s"))
                .append(Text.literal("§7."));

        Services.CHAT.sendPop(event.getPlayer().getUuid(), msg, true);
    }
}