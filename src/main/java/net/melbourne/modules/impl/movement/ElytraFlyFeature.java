package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.*;
import net.melbourne.mixins.accessors.PlayerMoveC2SPacketAccessor;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.miscellaneous.math.MathUtils;
import net.melbourne.utils.entity.player.movement.MovementUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@FeatureInfo(name = "ElytraFly", category = Category.Movement)
public class ElytraFlyFeature extends Feature {
    public ModeSetting mode = new ModeSetting("Mode", "The mode that will be used for elytra flying.", "Control", new String[]{"Packet", "Control", "Bounce"});

    public NumberSetting horizontal = new NumberSetting("Horizontal", "The speed at which you will be flying horizontally.", 2.0f, 0.1f, 10.0f,
            () -> !mode.getValue().equalsIgnoreCase("Bounce"));

    public NumberSetting vertical = new NumberSetting("Vertical", "The speed at which you will be flying vertically.", 1.0f, 0.1f, 10.0f,
            () -> !mode.getValue().equalsIgnoreCase("Bounce"));

    public BooleanSetting moveVertically = new BooleanSetting("MoveVertically", "Whether or not to allow for vertical movement.", true,
            () -> !mode.getValue().equalsIgnoreCase("Bounce"));

    public BooleanSetting infiniteDurability = new BooleanSetting("InfDura", "Prevents your elytra from having any durability used up.", false,
            () -> mode.getValue().equalsIgnoreCase("Packet"));

    public BooleanSetting stopOnGround = new BooleanSetting("StopOnGround", "Stops flying when you hit the ground.", true,
            () -> mode.getValue().equalsIgnoreCase("Packet"));

    public ModeSetting ncpStrict = new ModeSetting("NCPStrict", "Makes use of special bypasses for the NoCheatPlus anticheat.", "None", new String[]{"None", "Old", "New", "Motion"},
            () -> mode.getValue().equalsIgnoreCase("Packet"));

    public ModeSetting yawMode = new ModeSetting("YawMode", "The mode that will be used for elytra flying.", "Normal", new String[]{"Normal", "Smart"},
            () -> mode.getValue().equalsIgnoreCase("Bounce"));

    public BooleanSetting sprint = new BooleanSetting("Sprint", "Whether or not to allow for vertical movement.", true,
            () -> mode.getValue().equalsIgnoreCase("Bounce"));

    public BooleanSetting restart = new BooleanSetting("Restart", "Whether or not to allow for vertical movement.", true,
            () -> mode.getValue().equalsIgnoreCase("Bounce"));

    public NumberSetting restartDelay = new NumberSetting("RestartDelay", "The speed at which you will be flying vertically.", 7, 0, 20,
            () -> mode.getValue().equalsIgnoreCase("Bounce") && restart.getValue());

    public NumberSetting pitchVal = new NumberSetting("Pitch", "The speed at which you will be flying vertically.", 85, 0, 90,
            () -> mode.getValue().equalsIgnoreCase("Bounce"));

    int tickDelay = restartDelay.getValue().intValue();
    private float pitch;
    private boolean rubberbanded = false;

    public static boolean checkConditions(ClientPlayerEntity player) {
        ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
        return (!player.getAbilities().flying && !player.hasVehicle() && !player.isClimbing() && itemStack.contains(DataComponentTypes.GLIDER) && !itemStack.willBreakNextUse());
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mode.getValue().equalsIgnoreCase("Bounce")) {
            if (mc.options.jumpKey.isPressed() && !mc.player.isGliding())
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));

            if (checkConditions(mc.player)) {
                if (!rubberbanded) {
                    setPressed(mc.options.jumpKey, true);
                    setPressed(mc.options.forwardKey, true);

                    mc.getNetworkHandler().sendPacket(
                            new PlayerMoveC2SPacket.Full(
                                    mc.player.getX(),
                                    mc.player.getY(),
                                    mc.player.getZ(),
                                    getYawDirection(),
                                    pitchVal.getValue().floatValue(),
                                    mc.player.isOnGround(),
                                    mc.player.horizontalCollision
                            )
                    );
                }

                if (!sprint.getValue()) {
                    if (mc.player.isGliding())
                        mc.player.setSprinting(mc.player.isOnGround());
                    else
                        mc.player.setSprinting(true);
                }

                if (rubberbanded && restart.getValue()) {
                    if (tickDelay > 0) {
                        tickDelay--;
                    } else {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        rubberbanded = false;
                        tickDelay = restartDelay.getValue().intValue();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (mode.getValue().equalsIgnoreCase("Bounce")) {
            if (event.getPacket() instanceof ClientCommandC2SPacket && ((ClientCommandC2SPacket) event.getPacket()).getMode().equals(ClientCommandC2SPacket.Mode.START_FALL_FLYING) && !sprint.getValue()) {
                mc.player.setSprinting(true);
            }
        }
    }

    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        KeyBinding.setKeyPressed(key.getDefaultKey(), pressed);
    }

    private float getYawDirection() {
        return switch (yawMode.getValue()) {
            case "Normal" -> mc.player.getYaw();
            case "Smart" -> Math.round((mc.player.getYaw() + 1f) / 45f) * 45f;
            default -> mc.player.getYaw();
        };

    }

    @SubscribeEvent
    public void onPlayerMove(MoveEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.getValue().equalsIgnoreCase("Packet")) return;

        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlySpeed(0.05F);

        if ((mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, -0.3, 0.0)).iterator().hasNext() && stopOnGround.getValue()) || mc.player.getInventory().getStack(38).getItem() != Items.ELYTRA)
            return;

        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(horizontal.getValue().floatValue() / 15.0f);
        event.setCancelled(true);

        if (Math.abs(event.getX()) < 0.05) event.setX(0);
        if (Math.abs(event.getZ()) < 0.05) event.setZ(0);

        event.setY(moveVertically.getValue() ? mc.options.jumpKey.isPressed() ? vertical.getValue().doubleValue() : mc.options.sneakKey.isPressed() ? -vertical.getValue().doubleValue() : 0 : 0);

        switch (ncpStrict.getValue().toLowerCase()) {
            case "old" -> event.setY(0.0002 - (mc.player.age % 2 == 0 ? 0 : 0.000001) + MathUtils.random(0.0000009, 0));
            case "new" -> event.setY(-1.000088900582341E-12);
            case "motion" -> event.setY(-4.000355602329364E-12);
        }

        if (mc.player.horizontalCollision && (ncpStrict.getValue().equalsIgnoreCase("New") || ncpStrict.getValue().equalsIgnoreCase("Motion")) && mc.player.age % 2 == 0)
            event.setY(-0.07840000152587923);

        if (infiniteDurability.getValue() || ncpStrict.getValue().equalsIgnoreCase("Motion")) {
            if (!MovementUtils.isMoving() && Math.abs(event.getX()) < 0.121) {
                float angleToRad = (float) Math.toRadians(4.5 * (mc.player.age % 80));
                event.setX(Math.sin(angleToRad) * 0.12);
                event.setZ(Math.cos(angleToRad) * 0.12);
            }
        }
    }

    @SubscribeEvent
    public void onSendMovement(SendMovementEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.getValue().equalsIgnoreCase("Packet")) return;

        if ((!mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, -0.3, 0.0)).iterator().hasNext() || !stopOnGround.getValue()) && mc.player.getInventory().getStack(38).getItem() == Items.ELYTRA) {
            if (infiniteDurability.getValue() || !mc.player.isGliding())
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            if (mc.player.age % 3 != 0 && ncpStrict.getValue().equalsIgnoreCase("Motion")) event.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerTravel(PlayerTravelEvent event) {
        if (mc.player == null || mc.world == null || !mc.player.isGliding()) return;

        if (mode.getValue().equalsIgnoreCase("Control")) {
            event.setCancelled(true);

            if (mc.player.input.getMovementInput().x == 0.0f && mc.player.input.getMovementInput().y == 0.0f) {
                mc.player.setVelocity(new Vec3d(0.0, mc.player.getVelocity().getY(), 0.0));
            } else {
                pitch = 12;

                double cos = Math.cos(Math.toRadians(mc.player.getYaw() + 90.0f));
                double sin = Math.sin(Math.toRadians(mc.player.getYaw() + 90.0f));

                mc.player.setVelocity(new Vec3d(
                        (mc.player.input.getMovementInput().y * horizontal.getValue().doubleValue() * cos) + (mc.player.input.getMovementInput().x * horizontal.getValue().doubleValue() * sin),
                        mc.player.getVelocity().y,
                        (mc.player.input.getMovementInput().y * horizontal.getValue().doubleValue() * sin) - (mc.player.input.getMovementInput().x * horizontal.getValue().doubleValue() * cos)
                ));
            }

            mc.player.setVelocity(new Vec3d(mc.player.getVelocity().getX(), 0.0, mc.player.getVelocity().getZ()));

            if (moveVertically.getValue()) {
                if (mc.options.jumpKey.isPressed()) {
                    mc.player.setVelocity(new Vec3d(mc.player.getVelocity().getX(), vertical.getValue().doubleValue(), mc.player.getVelocity().getZ()));
                    pitch = -51;
                } else if (mc.options.sneakKey.isPressed()) {
                    mc.player.setVelocity(new Vec3d(mc.player.getVelocity().getX(), -vertical.getValue().doubleValue(), mc.player.getVelocity().getZ()));
                    pitch = 0;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.getValue().equalsIgnoreCase("Packet")) {
            if (event.getPacket() instanceof EntityTrackerUpdateS2CPacket(
                    int id, List<DataTracker.SerializedEntry<?>> trackedValues
            ) && id == mc.player.getId()) {
                if (trackedValues.isEmpty()) return;

                for (DataTracker.SerializedEntry<?> value : trackedValues) {
                    if (value.value().toString().equals("FALL_FLYING") || (value.id() == 0 && (value.value().toString().equals("-120") || value.value().toString().equals("-128") || value.value().toString().equals("-126")))) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        if (mode.getValue().equalsIgnoreCase("Control")) {
            if (event.getPacket() instanceof PlayerMoveC2SPacket packet && packet.changesLook() && mc.player.isGliding()) {
                if (mode.getValue().equalsIgnoreCase("Control")) {
                    if (mc.options.leftKey.isPressed())
                        ((PlayerMoveC2SPacketAccessor) packet).setYaw(packet.getYaw(0.0f) - 90.0f);
                    if (mc.options.rightKey.isPressed())
                        ((PlayerMoveC2SPacketAccessor) packet).setYaw(packet.getYaw(0.0f) + 90.0f);
                }

                ((PlayerMoveC2SPacketAccessor) packet).setPitch(pitch);
            }
        }

        if (mode.getValue().equalsIgnoreCase("Bounce")) {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                rubberbanded = true;
                mc.player.stopGliding();
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        rubberbanded = false;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlySpeed(0.05F);
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}