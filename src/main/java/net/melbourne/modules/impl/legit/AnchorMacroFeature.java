package net.melbourne.modules.impl.legit;

import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

@FeatureInfo(name = "AnchorMacro", category = Category.Legit)
public class AnchorMacroFeature extends Feature {

    private final NumberSetting switchDelay = new NumberSetting("SwitchDelay", "Ticks before switching items", 0, 0, 20);
    private final NumberSetting glowstoneDelay = new NumberSetting("GlowstoneDelay", "Ticks before placing glowstone", 0, 0, 20);
    private final NumberSetting explodeDelay = new NumberSetting("ExplodeDelay", "Ticks before exploding anchor", 0, 0, 20);
    private final NumberSetting totemSlot = new NumberSetting("ExplodeSlot", "Slot to switch to when exploding", 1, 1, 9);

    private int switchCounter = 0;
    private int glowCounter = 0;
    private int explodeCounter = 0;
    private boolean placedGlowstone = false;
    private boolean explodedAnchor = false;
    private BlockHitResult storedHit = null;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        switchCounter = 0;
        glowCounter = 0;
        explodeCounter = 0;
        placedGlowstone = false;
        explodedAnchor = false;
        storedHit = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;
        if (isUsingShieldOrFood()) return;
        boolean rightClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 1) == 1;
        if (!rightClick) {
            placedGlowstone = false;
            explodedAnchor = false;
            storedHit = null;
            return;
        }
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        storedHit = hit;
        BlockPos pos = hit.getBlockPos();
        if (!isBlockAtPosition(pos, Blocks.RESPAWN_ANCHOR)) return;
        mc.options.useKey.setPressed(false);
        if (isRespawnAnchorUncharged(pos) && !placedGlowstone) {
            placeGlowstone();
        } else if (isRespawnAnchorCharged(pos) && !explodedAnchor) {
            explodeAnchor();
        }
    }

    private boolean isUsingShieldOrFood() {
        boolean foodMain = mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
        boolean foodOff = mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
        boolean shieldMain = mc.player.getMainHandStack().getItem() instanceof ShieldItem;
        boolean shieldOff = mc.player.getOffHandStack().getItem() instanceof ShieldItem;
        boolean rightClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 1) == 1;
        return (foodMain || foodOff || shieldMain || shieldOff) && rightClick;
    }

    private void placeGlowstone() {
        Slot glowSlot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, Items.GLOWSTONE);
        if (glowSlot == null) return;
        int prev = mc.player.getInventory().getSelectedSlot();
        int slotIndex = glowSlot.getIndex();
        if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (switchCounter++ < switchDelay.getValue().intValue()) return;
            switchCounter = 0;
            mc.player.getInventory().setSelectedSlot(slotIndex);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slotIndex));
            return;
        }
        if (glowCounter++ < glowstoneDelay.getValue().intValue()) return;
        glowCounter = 0;
        if (storedHit != null) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, storedHit);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        placedGlowstone = true;
        mc.player.getInventory().setSelectedSlot(prev);
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prev));
    }

    private void explodeAnchor() {
        int targetSlot = totemSlot.getValue().intValue() - 1;
        int prev = mc.player.getInventory().getSelectedSlot();
        if (prev != targetSlot) {
            if (switchCounter++ < switchDelay.getValue().intValue()) return;
            switchCounter = 0;
            mc.player.getInventory().setSelectedSlot(targetSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
            return;
        }
        if (explodeCounter++ < explodeDelay.getValue().intValue()) return;
        explodeCounter = 0;
        if (storedHit != null) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, storedHit);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        explodedAnchor = true;
        mc.player.getInventory().setSelectedSlot(prev);
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prev));
    }

    private static boolean isBlockAtPosition(final BlockPos blockPos, final net.minecraft.block.Block block) {
        if (mc.world == null) return false;
        return mc.world.getBlockState(blockPos).isOf(block);
    }

    private static boolean isRespawnAnchorCharged(final BlockPos blockPos) {
        return isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR)
                && (int) mc.world.getBlockState(blockPos).get((Property) RespawnAnchorBlock.CHARGES) != 0;
    }

    private static boolean isRespawnAnchorUncharged(final BlockPos blockPos) {
        return isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR)
                && (int) mc.world.getBlockState(blockPos).get((Property) RespawnAnchorBlock.CHARGES) == 0;
    }
}
