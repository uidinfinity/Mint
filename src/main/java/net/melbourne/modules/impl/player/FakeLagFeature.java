package net.melbourne.modules.impl.player;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.*;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.*;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@FeatureInfo(name = "FakeLag", category = Category.Player)
public class FakeLagFeature extends Feature {
    protected final ArrayList<Packet<?>> full = new ArrayList<>();
    private final ArrayList<PlayerMoveC2SPacket> packets = new ArrayList<>();
    private final Timer timer = new Timer();
    private final Timer safety = new Timer();

    public ModeSetting mode = new ModeSetting("Mode", "The mode FakeLag uses.", "Pulse", new String[]{"Pulse", "Blink"});

    public NumberSetting factor = new NumberSetting("Factor", "The delay to choke packets for.", 2, 1, 5,
            () -> mode.getValue().equalsIgnoreCase("Pulse"));

    public BooleanSetting render = new BooleanSetting("Render", "Allows you to see your original location and trail.", true,
            () -> mode.getValue().equalsIgnoreCase("Blink"));

    private boolean sending = false;
    @Getter
    private OtherClientPlayerEntity player = null;

    private final CopyOnWriteArrayList<Vec3d> trail = new CopyOnWriteArrayList<>();
    private Vec3d start = null;

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull()) return;

        if (mode.getValue().equals("Pulse") && (
                event.getPacket() instanceof PlayerPositionLookS2CPacket ||
                        event.getPacket() instanceof DisconnectS2CPacket ||
                        (event.getPacket() instanceof HealthUpdateS2CPacket packet && packet.getHealth() <= 0)
        )) {
            sendPackets();
            safety.reset();
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (getNull() || sending) return;

        if (mode.getValue().equals("Blink")) {
            Packet<?> p = event.getPacket();

            if (p instanceof PlayerMoveC2SPacket) {
                event.setCancelled(true);
                if (!full.contains(p)) full.add(p);
            }
            return;
        }

        if (!shouldChoke() || !(event.getPacket() instanceof PlayerMoveC2SPacket packet))
            return;

        synchronized (packets) {
            event.setCancelled(true);
            packets.add(packet);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        if (mode.getValue().equals("Blink") && render.getValue()) {
            Vec3d currentPos = new Vec3d(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ());

            if (start == null) {
                start = currentPos;
                trail.add(start);
            } else {
                trail.add(currentPos);
            }
        }

        if (packets.isEmpty()) return;

        if (timer.hasTimeElapsed((int) (factor.getValue().floatValue() * 100)) && mode.getValue().equals("Pulse")) {
            sendPackets();
            timer.reset();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull() || !mode.getValue().equals("Blink") || !render.getValue() || trail.size() < 2)
            return;

        java.awt.Color trailColor = ColorUtils.getGlobalColor();

        for (int i = 1; i < trail.size(); i++) {
            try {
                Vec3d from = trail.get(i - 1);
                Vec3d to = trail.get(i);
                Renderer3D.renderLine(event.getContext(), from, to, trailColor, trailColor);
            } catch (Exception ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onSettingChange(SettingChangeEvent event) {
        if (event.getSetting() == mode) {
            setEnabled(false);
        }
    }

    @Override
    public void onEnable() {
        full.clear();
        packets.clear();
        trail.clear();
        start = null;
        timer.reset();

        if (mode.getValue().equals("Blink") && render.getValue()) {
            player = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), mc.player.getName().getString()));
            player.copyPositionAndRotation(mc.player);
            player.setId(-420420);
            mc.world.addEntity(player);
            player.tick();
        }
    }

    @Override
    public void onDisable() {
        if (getNull()) return;

        if (mode.getValue().equals("Pulse"))
            sendPackets();

        if (mode.getValue().equals("Blink")) {
            full.forEach(packet -> {
                if (packet != null)
                    mc.getNetworkHandler().sendPacket(packet);
            });
            full.clear();

            if (render.getValue() && player != null) {
                mc.world.removeEntity(player.getId(), Entity.RemovalReason.DISCARDED);
                player = null;
            }
        }

        trail.clear();
        start = null;
    }

    private void sendPackets() {
        synchronized (packets) {
            sending = true;
            for (PlayerMoveC2SPacket packet : packets)
                mc.getNetworkHandler().sendPacket(packet);
            packets.clear();
            sending = false;
        }
    }

    private boolean shouldChoke() {
        return (EntityUtils.getSpeed(mc.player, EntityUtils.SpeedUnit.KILOMETERS) >= 5 || mc.player.fallDistance > 0) && safety.hasTimeElapsed(1000);
    }

    @Override
    public String getInfo() {
        if (mode.getValue().equals("Pulse"))
            return (shouldChoke() ? Formatting.GREEN : Formatting.RED) + "Pulsing";
        return "";
    }
}
