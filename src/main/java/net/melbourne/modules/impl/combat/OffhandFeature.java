package net.melbourne.modules.impl.combat;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.services.impl.InventoryService;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.melbourne.utils.inventory.SwitchType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static net.melbourne.utils.inventory.SwitchType.PickUp;
import static net.melbourne.utils.inventory.SwitchType.Swap;

@FeatureInfo(name = "Offhand", category = Category.Combat)
public class OffhandFeature extends Feature {

    public final ModeSetting mode = new ModeSetting("Mode", "Item to prioritize in offhand", "Totem", new String[]{"Totem", "Crystal", "Gapple", "Sword"});
    public final NumberSetting health = new NumberSetting("Health", "Switch to totem below this health (incl. absorption)", 16.0, 1.0, 20.0);
    public final BooleanSetting swordGap = new BooleanSetting("SwordGap", "Use Gapple when holding sword + right-click", true);
    public final BooleanSetting totemGap = new BooleanSetting("TotemGap", "Use Gapple when holding totem + right-click", true);
    public final BooleanSetting smart = new BooleanSetting("Smart", "Force totem near end crystals", true);

    private final InventoryService inv = new InventoryService();
    private boolean totem = false;

    @Override
    public void onEnable() {
        totem = false;
    }

    @Override
    public void onDisable() {
        totem = false;
        inv.clearAction();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof HandledScreen<?>)) return;

        Item desired = getDesiredOffhandItem();
        if (totem) {
            desired = Items.TOTEM_OF_UNDYING;
            totem = false;
        }
        if (mc.player.getOffHandStack().isOf(desired)) return;

        boolean hasCursor = !mc.player.currentScreenHandler.getCursorStack().isEmpty();
        SwitchType type = hasCursor ? PickUp : Swap;

        if (!switchInventoryFirst(desired, type)) {
            if (desired != Items.TOTEM_OF_UNDYING) {
                switchInventoryFirst(Items.TOTEM_OF_UNDYING, type);
            }
        }
    }

    private boolean switchInventoryFirst(Item item, SwitchType type) {
        if (inv.switchTo(SearchLogic.IgnoreHotbar, false, type, item)) return true;
        return inv.switchTo(SearchLogic.All, false, type, item);
    }

    @SubscribeEvent
    public void onPacket(PacketReceiveEvent event) {
        if (getNull()) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getEntity(mc.world) == mc.player && packet.getStatus() == 35) {
            if (getTotalHealth() <= health.getValue().floatValue()) {
                totem = true;
            }
        }
    }

    private Item getDesiredOffhandItem() {
        if (getNull()) return Items.TOTEM_OF_UNDYING;
        float hp = getTotalHealth();
        float fallDmg = getFallDamage((float) mc.player.fallDistance);
        boolean lowHealth = hp <= health.getValue().floatValue();
        boolean fallRisk = fallDmg >= hp;
        if (lowHealth || fallRisk) return Items.TOTEM_OF_UNDYING;
        if (smart.getValue()) {
            List<EndCrystalEntity> entities = mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(6.0), e -> true).reversed();
            if (!entities.isEmpty()) return Items.TOTEM_OF_UNDYING;
        }
        boolean using = mc.options.useKey.isPressed();
        Item mainHand = mc.player.getMainHandStack().getItem();
        if (totemGap.getValue() && using && mainHand == Items.TOTEM_OF_UNDYING) {
            return getBestGapple();
        }
        if (swordGap.getValue() && using && isSword(mainHand)) {
            return getBestGapple();
        }
        return switch (mode.getValue()) {
            case "Crystal" -> Items.END_CRYSTAL;
            case "Gapple" -> getBestGapple();
            case "Sword" -> getBestSword();
            default -> Items.TOTEM_OF_UNDYING;
        };
    }

    private Item getBestGapple() {
        return inv.getCount(stack -> stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) > 0 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
    }

    private Item getBestSword() {
        if (inv.getCount(stack -> stack.isOf(Items.NETHERITE_SWORD)) > 0) return Items.NETHERITE_SWORD;
        if (inv.getCount(stack -> stack.isOf(Items.DIAMOND_SWORD)) > 0) return Items.DIAMOND_SWORD;
        if (inv.getCount(stack -> stack.isOf(Items.IRON_SWORD)) > 0) return Items.IRON_SWORD;
        if (inv.getCount(stack -> stack.isOf(Items.STONE_SWORD)) > 0) return Items.STONE_SWORD;
        if (inv.getCount(stack -> stack.isOf(Items.WOODEN_SWORD)) > 0) return Items.WOODEN_SWORD;
        return Items.NETHERITE_SWORD;
    }

    private boolean isSword(Item item) {
        return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD ||
                item == Items.IRON_SWORD || item == Items.STONE_SWORD ||
                item == Items.WOODEN_SWORD;
    }

    private float getTotalHealth() {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    private float getFallDamage(float distance) {
        if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || mc.player.isGliding()) return 0;
        StatusEffectInstance jumpBoost = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
        float offset = jumpBoost != null ? (jumpBoost.getAmplifier() + 1) : 0;
        int damage = MathHelper.ceil(distance - 3.0f - offset);
        return Math.max(damage, 0);
    }

    @Override
    public String getInfo() {
        return "" + inv.getCount(stack -> stack.isOf(Items.TOTEM_OF_UNDYING));
    }
}
