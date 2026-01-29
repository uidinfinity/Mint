package net.melbourne.utils.miscellaneous;

import net.melbourne.Managers;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.RenderHudEvent;
import net.melbourne.events.impl.RenderWorldEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.impl.client.RendersFeature;
import net.melbourne.utils.entity.EntityUtils;
import net.melbourne.utils.entity.player.socials.FriendManager;
import net.melbourne.utils.graphics.impl.Renderer2D;
import net.melbourne.utils.graphics.impl.Renderer3D;
import net.melbourne.utils.graphics.impl.font.FontUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WaypointUtils {
    private static final List<LogoutSpot> logoutSpots = new ArrayList<>();
    private static final List<PingSpot> pingSpots = new ArrayList<>();
    private static final Map<UUID, Integer> totemPops = new HashMap<>();
    private static final net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

    static RendersFeature renders = Managers.FEATURE.getFeatureFromClass(RendersFeature.class);

    public static void clearSpots() {
        logoutSpots.clear();
        pingSpots.clear();
        totemPops.clear();
    }

    public static LogoutSpot createLogout(PlayerEntity player, double tickProgress) {
        LogoutSpot spot = new LogoutSpot();
        spot.uuid = player.getUuid();
        spot.name = player.getName().getString();
        spot.pos = EntityUtils.getRenderPos((Entity) player, (float) tickProgress);
        spot.health = player.getHealth() + player.getAbsorptionAmount();
        spot.pops = totemPops.getOrDefault(player.getUuid(), 0);
        return spot;
    }

    public static PingSpot createPing(String name, Vec3d pos) {
        PingSpot spot = new PingSpot();
        spot.name = name;
        spot.pos = pos;
        spot.timestamp = System.currentTimeMillis();
        return spot;
    }

    public static void addPingSpot(UUID uuid, PingSpot spot) {
        pingSpots.removeIf(s -> s.uuid != null && s.uuid.equals(uuid));
        spot.uuid = uuid;
        pingSpots.add(spot);
    }

    public static void onTick(TickEvent event, boolean logouts, boolean pings) {
        if (mc.world == null || mc.player == null) return;
        long currentTime = System.currentTimeMillis();
        if (logouts) {
            List<UUID> currentPlayers = new ArrayList<>();
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player) currentPlayers.add(p.getUuid());
            }
            logoutSpots.removeIf(spot -> currentPlayers.contains(spot.uuid));
        }
        if (pings) {
            pingSpots.removeIf(spot -> currentTime - spot.timestamp >= 45000);
        }
    }

    public static void onPacket(PacketReceiveEvent event, boolean logouts) {
        if (!logouts || mc.world == null || mc.player == null) return;
        if (event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getEntity(mc.world) instanceof PlayerEntity player) {
                UUID uuid = player.getUuid();
                if (packet.getStatus() == 35) totemPops.merge(uuid, 1, Integer::sum);
                else if (packet.getStatus() == 3) totemPops.remove(uuid);
            }
        } else if (event.getPacket() instanceof PlayerRemoveS2CPacket packet) {
            for (UUID uuid : packet.profileIds()) {
                PlayerEntity player = mc.world.getPlayerByUuid(uuid);
                if (player != null && player != mc.player) {
                    logoutSpots.add(createLogout(player, mc.getRenderTickCounter().getTickProgress(true)));
                    totemPops.remove(uuid);
                }
            }
        } else if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                    logoutSpots.removeIf(spot -> spot.uuid.equals(entry.profile().getId()));
                    totemPops.remove(entry.profile().getId());
                }
            }
        }
    }

    public static void onRenderWorld(RenderWorldEvent event, boolean logouts, boolean pings, Color fillColor, Color outlineColor) {
        if (mc.world == null || mc.player == null) return;
        if (logouts) {
            for (LogoutSpot spot : new ArrayList<>(logoutSpots)) {
                Box box = new Box(spot.pos.x - 0.3, spot.pos.y, spot.pos.z - 0.3, spot.pos.x + 0.3, spot.pos.y + 1.8, spot.pos.z + 0.3);
                if (fillColor.getAlpha() > 0) Renderer3D.renderBox(event.getContext(), box, fillColor);
                if (outlineColor.getAlpha() > 0) Renderer3D.renderBoxOutline(event.getContext(), box, outlineColor);
            }
        }
        if (pings) {
            for (PingSpot spot : new ArrayList<>(pingSpots)) {
                Box box = new Box(spot.pos.x - 0.3, spot.pos.y, spot.pos.z - 0.3, spot.pos.x + 0.3, spot.pos.y + 1.8, spot.pos.z + 0.3);
                if (fillColor.getAlpha() > 0) Renderer3D.renderBox(event.getContext(), box, fillColor);
                if (outlineColor.getAlpha() > 0) Renderer3D.renderBoxOutline(event.getContext(), box, outlineColor);
            }
        }
    }

    private static String displayNameForPing(PingSpot spot, boolean useMint) {
        if (!useMint) return spot.name;
        if (spot.uuid == null) return spot.name;
        if (Managers.BOT == null) return spot.name;
        String mint = Managers.BOT.getMintNameFor(spot.uuid);
        if (mint == null || mint.trim().isEmpty()) return spot.name;
        return mint.trim();
    }

    public static void onRenderOverlay(RenderHudEvent event, boolean logouts, boolean pings, boolean showHealth, boolean showDistance, boolean showTotemPops, boolean pingMintNames, Color outlineColor) {
        if (mc.world == null || mc.player == null) return;
        Matrix3x2fStack matrices = event.getContext().getMatrices();
        DecimalFormat df = new DecimalFormat("0.0");
        FriendManager fm = Managers.FRIEND;

        if (logouts) {
            for (LogoutSpot spot : new ArrayList<>(logoutSpots)) {
                Vec3d projected = Renderer3D.project(spot.pos.add(0, 2.0, 0));
                if (!Renderer3D.projectionVisible(projected)) continue;

                MutableText nameText = Text.literal(spot.name);
                if (fm != null && fm.isFriend(spot.name)) nameText.setStyle(nameText.getStyle().withColor(TextColor.fromRgb(renders.getFriendColor().getRGB())));
                else nameText.formatted(Formatting.WHITE);

                MutableText main = nameText.append(Text.literal(" logout ").formatted(Formatting.RESET));
                if (showHealth) main.append(Text.literal(df.format(spot.health)).formatted(Formatting.GRAY));

                renderLabel(event, matrices, main.getString(), spot.pos, outlineColor, df, showDistance, showTotemPops, spot.pops);
            }
        }

        if (pings) {
            for (PingSpot spot : new ArrayList<>(pingSpots)) {
                Vec3d projected = Renderer3D.project(spot.pos.add(0, 2.0, 0));
                if (!Renderer3D.projectionVisible(projected)) continue;

                String shownName = displayNameForPing(spot, pingMintNames);

                MutableText nameText = Text.literal(shownName);
                if (fm != null && fm.isFriend(spot.name)) nameText.setStyle(nameText.getStyle().withColor(TextColor.fromRgb(renders.getFriendColor().getRGB())));
                else nameText.formatted(Formatting.WHITE);

                MutableText main = nameText.append(Text.literal("'s Ping").formatted(Formatting.WHITE));
                renderLabel(event, matrices, main.getString(), spot.pos, outlineColor, df, showDistance, false, 0);
            }
        }
    }

    private static void renderLabel(RenderHudEvent event, Matrix3x2fStack matrices, String text, Vec3d pos, Color outlineColor, DecimalFormat df, boolean showDist, boolean showPops, int pops) {
        String suffix = "";
        if (showDist || (showPops && pops > 0)) {
            suffix = " §c(";
            if (showDist) suffix += "§f" + df.format(mc.player.getPos().distanceTo(pos)) + "m";
            if (showDist && showPops && pops > 0) suffix += " ";
            if (showPops && pops > 0) suffix += "§e-" + pops;
            suffix += "§c)";
        }

        float totalWidth = FontUtils.getWidth(text + suffix);
        float dist = (float) mc.cameraEntity.squaredDistanceTo(pos);
        float scale = Math.max(0.5f, Math.min(1.0f, 20.0f / dist));

        matrices.pushMatrix();
        matrices.translate((float) Renderer3D.project(pos.add(0, 2.0, 0)).x, (float) Renderer3D.project(pos.add(0, 2.0, 0)).y);
        matrices.scale(scale, scale);
        Renderer2D.renderQuad(event.getContext(), -totalWidth / 2 - 1, -FontUtils.getHeight() - 2, totalWidth / 2 + 1, 0, new Color(0, 0, 0, 50));
        if (outlineColor.getAlpha() > 0) Renderer2D.renderOutline(event.getContext(), -totalWidth / 2 - 1, -FontUtils.getHeight() - 2, totalWidth / 2 + 1, 0, outlineColor);
        FontUtils.drawTextWithShadow(event.getContext(), text + suffix, -totalWidth / 2, -FontUtils.getHeight(), Color.WHITE);
        matrices.popMatrix();
    }

    public static class LogoutSpot { UUID uuid; String name; Vec3d pos; float health; int pops; }
    public static class PingSpot { UUID uuid; String name; Vec3d pos; long timestamp; }
}
