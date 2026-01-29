package net.melbourne.modules.impl.client;

import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.Managers;
import net.minecraft.util.Identifier;

@FeatureInfo(name = "Capes", category = Category.Client)
public final class CapesFeature extends Feature {

    private static final Identifier MELBOURNE_CAPE = Identifier.of("melbourne", "textures/melbourne.png");
    private static final Identifier PK_CAPE = Identifier.of("melbourne", "textures/pk.png");
    private static final Identifier EMP_CAPE = Identifier.of("melbourne", "textures/emp.png");

    private static final Identifier DOT5_CAPE = Identifier.of("melbourne", "textures/dot5.png");
    private static final Identifier SPOTIFY_CAPE = Identifier.of("melbourne", "textures/spotify.png");
    private static final Identifier BUTTERFLY_CAPE = Identifier.of("melbourne", "textures/butterfly.png");
    private static final Identifier HIGHLAND_CAPE = Identifier.of("melbourne", "textures/highland.png");
    private static final Identifier HLDARK_CAPE = Identifier.of("melbourne", "textures/hldark.png");

    public final ModeSetting mode = new ModeSetting("Cape", "Choose which cape to display when Capes is enabled.", "Melbourne", new String[]{"Melbourne", "Peacekeepers", "Emperium", "Dot5", "Spotify", "Butterfly", "Highland", "HlDark"}) {
        @Override
        public void setValue(String value) {
            super.setValue(value);

            if (isEnabled())
                Managers.BOT.sendMessageToIrc(String.format("[CAPE] UUID: %s, NAME: %s, MODE: %s", mc.player.getUuid(), mc.player.getName().getString(), mode.getValue()));
        }
    };

    @Override
    public void onEnable() {
        if (mc.player == null || Managers.BOT == null)
            return;

        Managers.BOT.sendMessageToIrc(String.format("[CAPE] UUID: %s, NAME: %s, MODE: %s", mc.player.getUuid(), mc.player.getName().getString(), mode.getValue()));
    }

    @Override
    public void onDisable() {
        if (mc.player == null || Managers.BOT == null)
            return;

        Managers.BOT.sendMessageToIrc(String.format("[CAPE] UUID: %s, NAME: %s, MODE: NONE", mc.player.getUuid(), mc.player.getName().getString()));
    }

    public Identifier getCapeTexture() {
        if (!isEnabled()) return null;
        return switch (mode.getValue()) {
            case "Melbourne" -> MELBOURNE_CAPE;
            case "Peacekeepers" -> PK_CAPE;
            case "Emperium" -> EMP_CAPE;
            case "Dot5" -> DOT5_CAPE;
            case "Spotify" -> SPOTIFY_CAPE;
            case "Butterfly" -> BUTTERFLY_CAPE;
            case "Highland" -> HIGHLAND_CAPE;
            case "HlDark" -> HLDARK_CAPE;
            default          -> MELBOURNE_CAPE;
        };
    }

    public static Identifier getCapeFor(String mode) {
        return switch (mode) {
            case "Melbourne" -> MELBOURNE_CAPE;
            case "Peacekeepers" -> PK_CAPE;
            case "Emperium" -> EMP_CAPE;
            case "Dot5" -> DOT5_CAPE;
            case "Spotify" -> SPOTIFY_CAPE;
            case "Butterfly" -> BUTTERFLY_CAPE;
            case "Highland" -> HIGHLAND_CAPE;
            case "HlDark" -> HLDARK_CAPE;
            default          -> MELBOURNE_CAPE;
        };
    }
}