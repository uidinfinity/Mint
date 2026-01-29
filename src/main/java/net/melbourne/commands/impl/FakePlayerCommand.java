package net.melbourne.commands.impl;

import com.mojang.authlib.GameProfile;
import net.melbourne.services.Services;
import net.melbourne.commands.Command;
import net.melbourne.commands.CommandInfo;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.UUID;

@CommandInfo(name = "FakePlayer", desc = "Spawns or removes a fake player.")
public class FakePlayerCommand extends Command {

    private OtherClientPlayerEntity fakePlayer = null;

    @Override
    public void onCommand(String[] args) {
        if (mc.world == null || mc.player == null) {
            Services.CHAT.sendRaw("§cWorld not loaded.");
            return;
        }

        if (fakePlayer != null) {
            mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
            Services.CHAT.sendRaw("§7Removed fake player with the name §s" + (args.length > 0 ? args[0] : "Mint"));
            return;
        }

        String name = args.length > 0 ? args[0] : "Mint";
        float health = 20.0f;
        if (args.length > 1) {
            try {
                health = Float.parseFloat(args[1]);
            } catch (NumberFormatException e) {
                Services.CHAT.sendRaw("§cInvalid health, using default 20.0");
            }
        }

        fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), name));
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.copyFrom(mc.player);
        fakePlayer.setId(-696969);
        fakePlayer.setHealth(health);

        mc.world.addEntity(fakePlayer);
        Services.CHAT.sendRaw("§7Spawned fake player with the name §s" + name);
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.world == null || mc.player == null || fakePlayer == null)
            return;

        if (fakePlayer.getHealth() <= 0) {
            fakePlayer.setHealth(20.0f);
            fakePlayer.clearStatusEffects();
        }

        damage(0.5f);
    }

    public void damage(float amount) {
        float newHealth = fakePlayer.getHealth() - amount;
        if (newHealth <= 0) {
            fakePlayer.setHealth(0.5f);
            fakePlayer.clearStatusEffects();
        } else {
            fakePlayer.setHealth(newHealth);
        }
    }
}
