package net.melbourne.services.impl;

import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.services.Service;
import net.melbourne.utils.Globals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.math.Box;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HitboxService extends Service implements Globals {
    private final List<Entity> serverCrawling = new CopyOnWriteArrayList<>();

    public HitboxService() {
        super("Hitbox", "Handles server hitboxes.");
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onPacketInbound(PacketReceiveEvent event)
    {
        if (mc.world == null) return;


        if (event.getPacket() instanceof EntityTrackerUpdateS2CPacket(int id, List<DataTracker.SerializedEntry<?>> trackedValues)) {
            Entity entity = mc.world.getEntityById(id);

            if (!(entity instanceof PlayerEntity))
                return;

            for (DataTracker.SerializedEntry<?> serializedEntry : trackedValues) {
                DataTracker.Entry<?> entry = entity.getDataTracker().entries[serializedEntry.id()];

                if (!entry.getData().equals(Entity.POSE))
                    continue;

                if (serializedEntry.value().equals(EntityPose.SWIMMING)) {
                    if (!serverCrawling.contains(entity))
                        serverCrawling.add(entity);
                } else {
                    serverCrawling.remove(entity);
                }
            }
        }
    }

    public boolean isServerCrawling(Entity entity)
    {
        return serverCrawling.contains(entity);
    }

    public Box getCrawlingBoundingBox(Entity entity)
    {
        return entity.getDimensions(EntityPose.SWIMMING).getBoxAt(entity.getPos());
    }
}