package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.modules.impl.client.CapesFeature;
import net.melbourne.utils.Globals;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin implements Globals {

    @Inject(method = "getSkinTextures", at = @At("TAIL"), cancellable = true)
    private void getSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        var capesFeature = Managers.FEATURE.getFeatureFromClass(CapesFeature.class);
        if (capesFeature == null || !capesFeature.isEnabled()) return;

        PlayerListEntry entry = (PlayerListEntry)(Object)this;
        UUID uuid = entry.getProfile().getId();

        Identifier capeTexture = null;

        if (mc.player != null && uuid.equals(mc.player.getGameProfile().getId())) {
            capeTexture = capesFeature.getCapeTexture();
        }


        else if (Managers.BOT != null) {
            String capeMode = Managers.BOT.getPlayerCape(uuid);
            if (capeMode != null && !capeMode.equals("NONE")) {
                capeTexture = CapesFeature.getCapeFor(capeMode);
            }
        }

        if (capeTexture != null) {
            SkinTextures original = cir.getReturnValue();
            SkinTextures newTextures = new SkinTextures(
                    original.texture(),
                    original.textureUrl(),
                    capeTexture,
                    capeTexture,
                    original.model(),
                    original.secure()
            );
            cir.setReturnValue(newTextures);
        }
    }
}