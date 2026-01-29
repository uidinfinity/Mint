package net.melbourne.modules.impl.combat;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.entity.DamageUtils;
import net.melbourne.utils.inventory.InventoryUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@FeatureInfo(name = "AutoArmor", category = Category.Combat)
public class AutoArmorFeature extends Feature {
    public ModeSetting health = new ModeSetting("Health", "The health priority to apply.", "Highest", new String[]{"Highest", "Lowest", "Any"});
    public BooleanSetting elytraPriority = new BooleanSetting("ElytraPriority", "Prioritizes elytra over armor pieces.", true);
    public BooleanSetting preserve = new BooleanSetting("Preserve", "Preserve low health armor to avoid it from breaking.", false);
    public NumberSetting preserveHealth = new NumberSetting("PreserveHealth", "The minimum health of armor to preserve it.", 20.0f, 10.0f, 50.0f, () -> preserve.getValue());
    public BooleanSetting elytra = new BooleanSetting("Elytra", "Equips an elytra instead of a chestplate.", false);

    public BooleanSetting autoMend = new BooleanSetting("AutoMend", "Automatically take off armor when fully mended.", false);
    public NumberSetting maxDurability = new NumberSetting("MaxDurability", "The durability threshold to take off armor.", 100.0f, 50.0f, 100.0f, () -> autoMend.getValue());
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "Range to force armor on if enemy is nearby.", 8.0f, 1.0f, 20.0f, () -> autoMend.getValue());
    public NumberSetting pearlRange = new NumberSetting("PearlRange", "Range to force armor on if pearl is nearby.", 8.0f, 1.0f, 20.0f, () -> autoMend.getValue());

    private int ticks = 0;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;

        if (ticks <= 0) {
            if (InventoryUtils.inInventoryScreen()) return;

            update(EquipmentSlot.HEAD, 5);
            if (!(elytraPriority.getValue() && mc.player.getInventory().getStack(38).getItem() == Items.ELYTRA) || elytra.getValue()) {
                update(EquipmentSlot.CHEST, 6);
            }
            update(EquipmentSlot.LEGS, 7);
            update(EquipmentSlot.FEET, 8);
        }

        ticks--;
    }

    private void update(EquipmentSlot type, int x) {
        int slot = type == EquipmentSlot.HEAD ? 39 : type == EquipmentSlot.CHEST ? 38 : type == EquipmentSlot.LEGS ? 37 : 36;
        ItemStack currentStack = mc.player.getInventory().getStack(slot);

        if (autoMend.getValue() && !shouldForceArmor()) {
            if (!currentStack.isEmpty() && getDurability(slot) >= maxDurability.getValue().floatValue()) {
                if (findEmptySlot() != -1) {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, x, 0, SlotActionType.QUICK_MOVE, mc.player);
                    ticks = 2;
                    return;
                }
            }

            if (isAllArmorMended()) return;
        }

        int elytraSlot = findElytra();
        boolean flag = elytra.getValue() && type == EquipmentSlot.CHEST;

        int armor = flag ? elytraSlot : findArmor(type);
        int best = flag ? compareElytra(38, armor) : compare(slot, armor, true);

        if (armor != -1 && best != slot) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, x, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, InventoryUtils.indexToSlot(armor), 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, x, 0, SlotActionType.PICKUP, mc.player);

            ticks = 2;
        }
    }

    private boolean shouldForceArmor() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && entity != mc.player) {
                if (Managers.FRIEND.isFriend(player.getName().getString())) continue;
                if (mc.player.distanceTo(player) <= enemyRange.getValue().floatValue()) return true;
            }

            if (entity instanceof EnderPearlEntity pearl) {
                if (pearl.getOwner() != null && Managers.FRIEND.isFriend(pearl.getOwner().getName().getString())) continue;
                if (mc.player.distanceTo(pearl) <= pearlRange.getValue().floatValue()) return true;
            }
        }
        return false;
    }

    private boolean isAllArmorMended() {
        for (int i = 36; i <= 39; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && getDurability(i) < maxDurability.getValue().floatValue()) {
                return false;
            }
        }
        return true;
    }

    private int findEmptySlot() {
        for (int i = 0; i <= 35; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private int compare(int x, int y, boolean swap) {
        if (y == -1) return x;

        ItemStack stackX = mc.player.getInventory().getStack(x);
        ItemStack stackY = mc.player.getInventory().getStack(y);

        if (!stackX.contains(DataComponentTypes.EQUIPPABLE)) return y;

        if (DamageUtils.getProtectionAmount(stackX) < DamageUtils.getProtectionAmount(stackY)) {
            return y;
        }

        if (preserve.getValue() && getDurability(x) < preserveHealth.getValue().floatValue()) {
            return getDurability(x) < getDurability(y) ? y : x;
        }

        if (!swap) {
            if (health.getValue().equals("Highest") && getDurability(x) < getDurability(y)) return y;
            else if (health.getValue().equals("Lowest") && getDurability(x) > getDurability(y)) return y;
        }

        return x;
    }

    private int compareElytra(int x, int y) {
        if (y == -1) return x;
        if (mc.player.getInventory().getStack(x).getItem() != Items.ELYTRA) return y;

        if (health.getValue().equals("Highest") && getDurability(x) < getDurability(y)) return y;
        else if (health.getValue().equals("Lowest") && getDurability(x) > getDurability(y)) return y;

        return x;
    }

    private int findArmor(EquipmentSlot type) {
        int slot = -1;
        for (int i = 0; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            EquipmentSlot itemType = getSlotType(stack);
            if (itemType != null && itemType.equals(type)) {
                slot = compare(i, slot, false);
            }
        }
        return slot;
    }

    private int findElytra() {
        int slot = -1;
        for (int i = 0; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != Items.ELYTRA) continue;
            slot = compareElytra(i, slot);
        }
        return slot;
    }

    private float getDurability(int slot) {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        if (!stack.isDamageable()) return 100.0f;
        return ((stack.getMaxDamage() - stack.getDamage()) * 100.0f) / stack.getMaxDamage();
    }

    private EquipmentSlot getSlotType(ItemStack itemStack) {
        if (itemStack.isEmpty()) return null;
        if (itemStack.contains(DataComponentTypes.GLIDER)) return EquipmentSlot.CHEST;
        var equippable = itemStack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null ? equippable.slot() : null;
    }
}