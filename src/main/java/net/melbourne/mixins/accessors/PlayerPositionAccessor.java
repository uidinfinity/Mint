package net.melbourne.mixins.accessors;

import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(PlayerPosition.class)
public interface PlayerPositionAccessor {
    @Accessor("yaw")
    @Mutable
    void setYaw(float yaw);

    @Accessor("pitch")
    @Mutable
    void setPitch(float pitch);

}