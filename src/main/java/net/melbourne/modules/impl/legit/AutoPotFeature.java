package net.melbourne.modules.impl.legit;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@FeatureInfo(name = "AutoPot", category = Category.Legit)
public class AutoPotFeature extends Feature {

    public BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "Disables after throwing a potion", true);
    public BooleanSetting groundCheck = new BooleanSetting("GroundCheck", "Only throw potions on the ground", true);
    public ModeSetting mode = new ModeSetting("Mode", "The potion throw mode", "Modern", new String[]{"Modern", "Legit"});
    public NumberSetting healthThreshold = new NumberSetting("HealthThreshold", "Health value to maintain using potions", 14.0, 1, 20);
    public ModeSetting potionMode = new ModeSetting("PotionMode", "Which potion to throw", "Health", new String[]{"Health", "Speed", "Strength", "TurtleMaster", "Smart"});
    private final ModeSetting switching = new ModeSetting("Switch", "The mode that the module uses to switch to the item you need to use.", "Silent", new String[]{"None", "Normal", "Silent", "Swap", "Pickup"});

    public BooleanSetting persistent = new BooleanSetting("Persistent", "Re-throws when effect ends", false);
    public NumberSetting retriggerMs = new NumberSetting("RetriggerMs", "Delay between throws", 200.0, 50.0, 2000.0);

    public BooleanSetting rotate = new BooleanSetting("Rotate", "Spoof rotations when throwing", true);
    public NumberSetting throwPitch = new NumberSetting("ThrowPitch", "Pitch to throw at", 90.0, 0.0, 90.0);

    private String trackedType = null;
    private long lastThrowMs = 0;
    private boolean persistentPrimed = false;

    @Override
    public void onEnable() {
        if (getNull()) {
            setToggled(false);
            return;
        }
        trackedType = null;
        lastThrowMs = 0;
        persistentPrimed = false;
    }

    @Override
    public void onDisable() {
        trackedType = null;
        lastThrowMs = 0;
        persistentPrimed = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (groundCheck.getValue() && !player.isOnGround()) return;

        long now = System.currentTimeMillis();
        if (now - lastThrowMs < retriggerMs.getValue().longValue()) return;

        String selected = potionMode.getValue();

        if (persistent.getValue()) {
            if ("Health".equalsIgnoreCase(selected)) {
                if (player.getHealth() < healthThreshold.getValue().doubleValue()) {
                    int idx = findPotionInvIndex("Health");
                    if (idx != -1 && throwPotionFromInventorySlot(idx)) {
                        lastThrowMs = now;
                        trackedType = "Health";
                    }
                }
                return;
            }

            String desired = "Smart".equalsIgnoreCase(selected) ? pickSmartType(player) : selected;
            if (desired == null) return;

            if (trackedType == null || !trackedType.equalsIgnoreCase(desired)) {
                trackedType = desired;
                persistentPrimed = false;
            }

            int idx = findPotionInvIndex(trackedType);
            if (idx == -1) {
                trackedType = null;
                persistentPrimed = false;
                return;
            }

            if (!persistentPrimed) {
                if (throwPotionFromInventorySlot(idx)) {
                    lastThrowMs = now;
                    persistentPrimed = true;
                }
                return;
            }

            if (hasEffectActive(player, trackedType)) return;

            if (throwPotionFromInventorySlot(idx)) {
                lastThrowMs = now;
            }

            return;
        }

        if ("Health".equalsIgnoreCase(selected)) {
            if (player.getHealth() < healthThreshold.getValue().doubleValue()) {
                int idx = findPotionInvIndex("Health");
                if (idx != -1 && throwPotionFromInventorySlot(idx)) {
                    lastThrowMs = now;
                } else {
                    if (autoDisable.getValue()) setToggled(false);
                }
            } else {
                if (autoDisable.getValue()) setToggled(false);
            }
            if (autoDisable.getValue()) setToggled(false);
            return;
        }

        String typeToThrow = "Smart".equalsIgnoreCase(selected) ? pickSmartType(player) : selected;
        if (typeToThrow == null) {
            if (autoDisable.getValue()) setToggled(false);
            return;
        }

        int idx = findPotionInvIndex(typeToThrow);
        if (idx == -1) {
            if (autoDisable.getValue()) setToggled(false);
            return;
        }

        if (throwPotionFromInventorySlot(idx)) {
            lastThrowMs = now;
            trackedType = typeToThrow;
        }

        if (autoDisable.getValue()) setToggled(false);
    }

    private boolean throwPotionFromInventorySlot(int invIndex) {
        if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) return false;

        ClientPlayerEntity player = mc.player;

        int selected = player.getInventory().getSelectedSlot();
        int fromHandlerSlot = invIndexToPlayerHandlerSlot(invIndex);

        ItemStack invStack = player.getInventory().getStack(invIndex);
        if (invStack == null || invStack.isEmpty()) return false;
        if (!(invStack.getItem() instanceof SplashPotionItem)) return false;

        float preYaw = player.getYaw();
        float prePitch = player.getPitch();

        float yawToUse = preYaw;
        float pitchToUse = (float) throwPitch.getValue().doubleValue();

        mc.interactionManager.clickSlot(0, fromHandlerSlot, selected, SlotActionType.SWAP, player);

        if (rotate.getValue()) {
            spoofLook(yawToUse, pitchToUse);
        }

        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                getSequenceId(),
                yawToUse,
                pitchToUse
        ));
        player.swingHand(Hand.MAIN_HAND);

        if (rotate.getValue()) {
            spoofLook(preYaw, prePitch);
        }

        mc.interactionManager.clickSlot(0, fromHandlerSlot, selected, SlotActionType.SWAP, player);

        return true;
    }

    private void spoofLook(float yaw, float pitch) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        ClientPlayerEntity p = mc.player;
        p.setYaw(yaw);
        p.setPitch(pitch);

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw,
                pitch,
                p.isOnGround(),
                p.horizontalCollision
        ));
    }

    private int invIndexToPlayerHandlerSlot(int invIndex) {
        if (invIndex >= 0 && invIndex < 9) return 36 + invIndex;
        return invIndex;
    }

    private String pickSmartType(ClientPlayerEntity player) {
        if (!hasEffectActive(player, "Strength") && findPotionInvIndex("Strength") != -1) return "Strength";
        if (!hasEffectActive(player, "Speed") && findPotionInvIndex("Speed") != -1) return "Speed";
        if (!hasEffectActive(player, "TurtleMaster") && findPotionInvIndex("TurtleMaster") != -1) return "TurtleMaster";
        return null;
    }

    private boolean hasEffectActive(ClientPlayerEntity player, String type) {
        if ("Speed".equalsIgnoreCase(type)) {
            StatusEffectInstance inst = player.getStatusEffect(StatusEffects.SPEED);
            return inst != null && inst.getDuration() > 0;
        }
        if ("Strength".equalsIgnoreCase(type)) {
            StatusEffectInstance inst = player.getStatusEffect(StatusEffects.STRENGTH);
            return inst != null && inst.getDuration() > 0;
        }
        if ("TurtleMaster".equalsIgnoreCase(type)) {
            StatusEffectInstance res = player.getStatusEffect(StatusEffects.RESISTANCE);
            return res != null && res.getDuration() > 0;
        }
        return false;
    }

    private int findPotionInvIndex(String type) {
        if (mc.player == null) return -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (matchesPotion(type, stack)) return i;
        }

        return -1;
    }

    private boolean matchesPotion(String type, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof SplashPotionItem)) return false;

        String name = stack.getName().getString().toLowerCase();

        if ("Health".equalsIgnoreCase(type)) return name.contains("healing");
        if ("Speed".equalsIgnoreCase(type)) return name.contains("swiftness");
        if ("Strength".equalsIgnoreCase(type)) return name.contains("strength");
        if ("TurtleMaster".equalsIgnoreCase(type)) return name.contains("turtle");
        if ("Smart".equalsIgnoreCase(type)) return name.contains("healing") || name.contains("swiftness") || name.contains("strength") || name.contains("turtle");

        return false;
    }

    private int getSequenceId() {
        return (int) (mc.world.getTime() % 1000);
    }

    @Override
    public String getInfo() {
        return mode.getValue();
    }
}
