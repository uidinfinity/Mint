package net.melbourne.modules.impl.render;

import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Set;

@FeatureInfo(name = "NoRender", category = Category.Render)
public class NoRenderFeature extends Feature {
    public BooleanSetting hurtCamera = new BooleanSetting("HurtCamera", "Disables the rendering of the hurt camera.", true);
    public BooleanSetting explosions = new BooleanSetting("Explosions", "Disables the rendering of explosion particles.", true);
    public BooleanSetting fireOverlay = new BooleanSetting("FireOverlay", "Disables the rendering of the fire overlay.", true);
    public BooleanSetting blockOverlay = new BooleanSetting("BlockOverlay", "Disables the rendering of the block suffocation overlay.", false);
    public BooleanSetting liquidOverlay = new BooleanSetting("LiquidOverlay", "Disables the rendering of the liquid overlay.", false);
    public BooleanSetting snowOverlay = new BooleanSetting("SnowOverlay", "Disables the rendering of the snow overlay.", false);
    public BooleanSetting pumpkinOverlay = new BooleanSetting("PumpkinOverlay", "Disables the rendering of the pumpkin overlay.", true);
    public BooleanSetting portalOverlay = new BooleanSetting("PortalOverlay", "Disables the rendering of the portal overlay.", false);
    public BooleanSetting totemAnimation = new BooleanSetting("TotemAnimation", "Disables the rendering of the totem pop animation.", false);
    public BooleanSetting bossBar = new BooleanSetting("BossBar", "Disables the rendering of the boss bar.", false);
    public BooleanSetting vignette = new BooleanSetting("Vignette", "Disables the rendering of the vignette.", true);
    public BooleanSetting blindness = new BooleanSetting("Blindness", "Disables the rendering of the blindness and darkness potion effects.", true);
    public BooleanSetting signText = new BooleanSetting("SignText", "Disables the rendering of sign text.", false);
    public BooleanSetting armor = new BooleanSetting("Armor", "Disables the rendering of armor.", false);
    public BooleanSetting smallShadow = new BooleanSetting("SmallShadow", "Small.", false);
    public BooleanSetting weather = new BooleanSetting("Weather", "Disables the rendering of rasin and thunder.", true);
    public BooleanSetting fog = new BooleanSetting("Fog", "Disables the rendering of fog.", false);
    public ModeSetting tileEntities = new ModeSetting("TileEntities", "Disables the rendering of tile entities, such as chests, when meeting requirements.", "Never", new String[]{"Never", "Distance", "Always"});
    public NumberSetting tileDistance = new NumberSetting("TileDistance", "The distance at which tile entities will stop rendering.", 10.0f, 0.0f, 100.0f, tileEntities.getValue().equalsIgnoreCase("Distance"));

    public final BooleanSetting unicode = new BooleanSetting("NoUnicode", "Blocks unicode messages", false);
    public final BooleanSetting particles = new BooleanSetting("AntiCrashParticles", "Blocks heavy particle packets", false);
    public final BooleanSetting sounds = new BooleanSetting("AntiCrashSounds", "Blocks lag-inducing sounds", false);

    private static final Set<SoundEvent> LAG_SOUNDS = Set.of(
            SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value(),
            SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value(),
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(),
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value(),
            SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(),
            SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value(),
            SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(),
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value()
    );

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull()) return;
        var packet = event.getPacket();

        if (unicode.getValue() && packet instanceof ChatMessageS2CPacket chat) {
            String txt = chat.body().content().toString();
            int flags = 0;

            for (char c : txt.toCharArray())
                if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN)
                    flags++;

            if (flags > 20) {
                Services.CHAT.sendRaw("Blocked unicode message with §s" + flags + "§rflags.");
                event.setCancelled(true);
            }
        }

        if (particles.getValue() && packet instanceof ParticleS2CPacket p) {
            int count = p.getCount();

            if (count > 800) {
                Services.CHAT.sendRaw("Blocked potential particle crash with §s" + count + "§rpackets.");
                event.setCancelled(true);
            }
        }

        if (sounds.getValue()) {
            if (packet instanceof PlaySoundFromEntityS2CPacket s1 && LAG_SOUNDS.contains(s1.getSound().value())) {
                event.setCancelled(true);
            }
            if (packet instanceof PlaySoundS2CPacket s2 && LAG_SOUNDS.contains(s2.getSound().value())) {
                event.setCancelled(true);
            }
        }
    }
}
