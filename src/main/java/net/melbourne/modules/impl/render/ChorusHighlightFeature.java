package net.melbourne.modules.impl.render;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ColorSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

@FeatureInfo(name = "ChorusHighlight", category = Category.Render)
public class ChorusHighlightFeature extends Feature {

    public NumberSetting duration = new NumberSetting("Duration", "How long the highlight stays full.", 2000, 500, 5000);
    public NumberSetting fade = new NumberSetting("Fade", "Fade out time in ms.", 500, 0, 2000);
    public ModeSetting renderMode = new ModeSetting("RenderMode", "What to render.", "Both", new String[]{"Fill", "Outline", "Both"});

    public ColorSetting fillColor = new ColorSetting("Fill", "Fill color.", new Color(191, 64, 191, 100),
            () -> renderMode.getValue().equals("Fill") || renderMode.getValue().equals("Both"));

    public ColorSetting outlineColor = new ColorSetting("Outline", "Outline color.", new Color(255, 255, 255),
            () -> renderMode.getValue().equals("Outline") || renderMode.getValue().equals("Both"));

    public BooleanSetting self = new BooleanSetting("Self", "Highlight own teleports.", true);

    private final ConcurrentHashMap<Vec3d, ChorusData> chorusPositions = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull()) return;

        if (event.getPacket() instanceof PlaySoundS2CPacket packet) {
            if (packet.getSound().value() == SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT) {
                Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

                if (!self.getValue()) {
                    if (mc.player.getPos().distanceTo(pos) < 1.5) return;
                }

                chorusPositions.put(pos, new ChorusData(System.currentTimeMillis()));
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull() || chorusPositions.isEmpty()) return;

        long now = System.currentTimeMillis();
        float maxTime = duration.getValue().floatValue();
        float fadeTime = fade.getValue().floatValue();

        chorusPositions.forEach((pos, data) -> {
            long age = now - data.startTime;

            if (age > (maxTime + fadeTime)) {
                chorusPositions.remove(pos);
                return;
            }

            float alphaMult = 1.0f;
            if (age > maxTime) {
                alphaMult = 1.0f - ((age - maxTime) / fadeTime);
            }

            renderChorusBox(event, pos, alphaMult);
        });
    }

    private void renderChorusBox(RenderWorldEvent event, Vec3d pos, float alphaMult) {
        Box box = new Box(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.8, pos.z + 0.3);

        Color f = fillColor.getColor();
        Color o = outlineColor.getColor();

        Color finalFill = new Color(f.getRed(), f.getGreen(), f.getBlue(), (int) (f.getAlpha() * alphaMult));
        Color finalLine = new Color(o.getRed(), o.getGreen(), o.getBlue(), (int) (o.getAlpha() * alphaMult));

        if (renderMode.getValue().equalsIgnoreCase("Fill") || renderMode.getValue().equalsIgnoreCase("Both"))
            Renderer3D.renderBox(event.getContext(), box, finalFill);
        if (renderMode.getValue().equalsIgnoreCase("Outline") || renderMode.getValue().equalsIgnoreCase("Both"))
            Renderer3D.renderBoxOutline(event.getContext(), box, finalLine);
    }

    private static class ChorusData {
        long startTime;
        ChorusData(long startTime) {
            this.startTime = startTime;
        }
    }
}