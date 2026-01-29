package net.melbourne.services.impl;

import lombok.Getter;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.KeyboardTickEvent;
import net.melbourne.events.impl.LocationEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.modules.impl.client.AntiCheatFeature;
import net.melbourne.services.Service;
import net.melbourne.utils.Globals;
import net.melbourne.utils.rotation.RotationPoint;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RotationService extends Service implements Globals {
    @Getter
    private float serverYaw, serverPitch, prevYaw, prevPitch;
    private final List<RotationPoint> points = new ArrayList<>();
    @Nullable
    private RotationPoint current;
    @Getter
    private boolean rotating;
    private int rotatedTicks;

    public RotationService() {
        super("Rotation", "Handles server-side rotations for modules.");
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent(priority = Integer.MAX_VALUE - 2)
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null)
            return;

        if (event.getPacket() instanceof PlayerMoveC2SPacket playerMoveC2SPacket && playerMoveC2SPacket.changesLook()) {
            prevYaw = serverYaw;
            prevPitch = serverPitch;
            serverYaw = playerMoveC2SPacket.getYaw(0.0f);
            serverPitch = playerMoveC2SPacket.getPitch(0.0f);
        }
    }

    public void update() {
        points.removeIf(Objects::isNull);

        if (points.isEmpty()) {
            current = null;
            rotating = false;
            rotatedTicks = 0;
            return;
        }

        RotationPoint prioritisedRotation = getPrioritisedRotation();
        if (prioritisedRotation == null) {
            current = null;
            rotating = false;
            rotatedTicks = 0;
            return;
        } else {
            current = prioritisedRotation;
        }

        rotatedTicks = 0;
        rotating = true;
    }

    @SubscribeEvent(priority = Integer.MAX_VALUE - 1)
    public void onLocation(LocationEvent event) {
        if (current != null && rotating) {
            event.setCancelled(true);
            event.setYaw(current.getYaw());
            event.setPitch(current.getPitch());

            if (current.isInstant()) {
                points.remove(current);
                current = null;
            }
            rotating = false;
        }
    }

    @SubscribeEvent(priority = Integer.MAX_VALUE - 2)
    public void onKeyboardTick(KeyboardTickEvent event) {
        if (mc.player == null || mc.world == null || mc.player.isRiding()) return;
        if (!Managers.FEATURE.getFeatureFromClass(AntiCheatFeature.class).moveFix.getValue()) return;
        if (current == null) return;

        float movementForward = event.getForward();
        float movementSideways = event.getSideways();

        float delta = (mc.player.getYaw() - current.getYaw()) * MathHelper.RADIANS_PER_DEGREE;

        float cos = MathHelper.cos(delta);
        float sin = MathHelper.sin(delta);

        event.setForward(Math.round(movementForward * cos + movementSideways * sin));
        event.setSideways(Math.round(movementSideways * cos - movementForward * sin));
        event.setCancelled(true);
    }

    public void setRotationPoint(RotationPoint rotationPoint) {
        if (rotationPoint == null) {
            points.clear();
            return;
        }

        RotationPoint toAdd = points.stream()
                .filter(Objects::nonNull)
                .filter(rp -> rotationPoint.getPriority() == rp.getPriority())
                .findFirst()
                .orElse(null);

        if (toAdd == null) {
            points.add(rotationPoint);
        } else {
            toAdd.setYaw(rotationPoint.getYaw());
            toAdd.setPitch(rotationPoint.getPitch());
            toAdd.setInstant(rotationPoint.isInstant());
        }
    }

    private RotationPoint getPrioritisedRotation() {
        Optional<RotationPoint> rotationPoint = points.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(RotationPoint::getPriority));
        return rotationPoint.orElse(null);
    }

    public boolean isRotationLate(int priority) {
        return current != null && current.getPriority() > priority;
    }
}