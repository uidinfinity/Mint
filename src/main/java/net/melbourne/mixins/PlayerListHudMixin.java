package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.render.ExtraTabFeature;
import net.melbourne.utils.Globals;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin implements Globals {

    @Shadow @Final
    private static Comparator<PlayerListEntry> ENTRY_ORDERING;

    @Inject(method = "collectPlayerEntries", at = @At("HEAD"), cancellable = true)
    private void collectPlayerEntries(CallbackInfoReturnable<List<PlayerListEntry>> info) {
        var extraTab = Managers.FEATURE.getFeatureFromClass(ExtraTabFeature.class);

        if (extraTab.isEnabled()) {
            var entries = mc.player.networkHandler.getListedPlayerListEntries().stream()
                    .sorted(ENTRY_ORDERING)
                    .limit(extraTab.limit.getValue().longValue())
                    .toList();

            info.setReturnValue(entries);
        }
    }

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    private void setPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (mc.player == null) return;

        var extraTab = Managers.FEATURE.getFeatureFromClass(ExtraTabFeature.class);
        if (!extraTab.isEnabled()) return;

        String playerName = entry.getProfile().getName();
        String selfName = mc.player.getName().getString();

        if (playerName.equals(selfName)) {
            MutableText text = Text.literal(selfName)
                    .setStyle(Text.literal("").getStyle().withColor(
                            extraTab.selfColor.getColor().getRGB()
                    ));
            cir.setReturnValue(text);
            cir.cancel();
        }
    }
}

