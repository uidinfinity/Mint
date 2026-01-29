package net.melbourne.services.impl;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.PlayerDeathEvent;
import net.melbourne.events.impl.PlayerPopEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.services.Service;
import net.melbourne.utils.Globals;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class WorldService extends Service implements Globals {
    private final Timer placeTimer = new Timer();
    @Setter
    private float timerMultiplier = 1.0f;

    private static final Vec3i[] SPHERE = new Vec3i[4187707];
    private static final int[] INDICES = new int[101];

    @Getter
    private final Map<UUID, Integer> poppedTotems = new ConcurrentHashMap<>();
    private final List<UUID> deadPlayers = new ArrayList<>();

    public WorldService() {
        super("World", "Does world stuff");
        Melbourne.EVENT_HANDLER.subscribe(this);

        BlockPos origin = BlockPos.ORIGIN;
        Set<BlockPos> positions = new TreeSet<>((o, p) -> {
            if (o.equals(p)) return 0;

            int compare = Double.compare(origin.getSquaredDistance(o), origin.getSquaredDistance(p));
            if (compare == 0) compare = Integer.compare(Math.abs(o.getX()) + Math.abs(o.getY()) + Math.abs(o.getZ()), Math.abs(p.getX()) + Math.abs(p.getY()) + Math.abs(p.getZ()));

            return compare == 0 ? 1 : compare;
        });

        for (int x = origin.getX() - 100; x <= origin.getX() + 100; x++) {
            for (int z = origin.getZ() - 100; z <= origin.getZ() + 100; z++) {
                for (int y = origin.getY() - 100; y < origin.getY() + 100; y++) {
                    double distance = (origin.getX() - x) * (origin.getX() - x) + (origin.getZ() - z) * (origin.getZ() - z) + (origin.getY() - y) * (origin.getY() - y);
                    if (distance < MathHelper.square(100)) positions.add(new BlockPos(x, y, z));
                }
            }
        }

        int i = 0;
        int currentDistance = 0;

        for (BlockPos position : positions) {
            if (Math.sqrt(origin.getSquaredDistance(position)) > currentDistance) INDICES[currentDistance++] = i;
            SPHERE[i++] = position;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.world == null) return;

        for (UUID uuid : new ArrayList<>(poppedTotems.keySet())) {
            if (mc.world.getPlayers().stream().noneMatch(player -> player.getUuid().equals(uuid))) {
                poppedTotems.remove(uuid);
            }
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.deathTime <= 0 && player.getHealth() > 0) {
                deadPlayers.remove(player.getUuid());
                continue;
            }

            if (deadPlayers.contains(player.getUuid()))
                continue;

            Melbourne.EVENT_HANDLER.post(new PlayerDeathEvent(player));

            deadPlayers.add(player.getUuid());

            poppedTotems.remove(player.getUuid());
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.world == null) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;

            int pops = poppedTotems.getOrDefault(player.getUuid(), 0);
            poppedTotems.put(player.getUuid(), ++pops);

            Melbourne.EVENT_HANDLER.post(new PlayerPopEvent(player, pops));
        }
    }

    public int getRadius(double radius) {
        return INDICES[MathHelper.clamp((int) Math.ceil(radius), 0, INDICES.length)];
    }

    public Vec3i getOffset(int index) {
        return SPHERE[index];
    }

    /**
     * Resets the timer multiplier to 1.0f.
     * Used by modules to safely return to normal speed.
     */
    public void resetTimerMultiplier() {
        this.setTimerMultiplier(1.0f);
    }

    public List<BlockPos> getSphere(BlockPos center, int range, boolean hollow) {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (x * x + y * y + z * z <= range * range) {
                        if (hollow && x * x + y * y + z * z < (range - 1) * (range - 1)) {
                            continue;
                        }
                        positions.add(center.add(x, y, z));
                    }
                }
            }
        }
        return positions;
    }
}