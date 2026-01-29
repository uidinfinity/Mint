package net.melbourne.modules.impl.misc;

import net.melbourne.Managers;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.inventory.kit.Kit;
import net.melbourne.utils.inventory.kit.Kit.KitItem;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

@FeatureInfo(name = "AutoRegear", category = Category.Misc)
public class AutoRegearFeature extends PlaceFeature {

    public final BooleanSetting place = new BooleanSetting("Place", "", false);
    public final BooleanSetting closeAfter = new BooleanSetting("Close", "", false);
    public final NumberSetting speed = new NumberSetting("Speed", "", 100, 1, 1000);
    public final NumberSetting clicks = new NumberSetting("Clicks", "", 1, 1, 5);

    private final Map<Integer, KitItem> expectedInv = new HashMap<>();
    private long lastActionTime = 0;

    private enum State { IDLE, PLACING, OPENING, GEARING, DONE }
    private State state = State.IDLE;
    private BlockPos placedPos = null;
    private int openTicks = 0;

    @Override
    public void onEnable() {
        super.onEnable();
        setup();
        state = place.getValue() ? State.PLACING : State.IDLE;
        placedPos = null;
        lastActionTime = 0;
        openTicks = 0;
    }

    @Override
    public void onDisable() {
        expectedInv.clear();
        state = State.IDLE;
        placedPos = null;
        super.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;
        if (expectedInv.isEmpty()) return;

        if (!place.getValue()) {
            handleRegearInOpenShulker(false);
            return;
        }

        handlePlaceFlow();
    }

    private void handlePlaceFlow() {
        if (state == State.DONE) return;

        if (state == State.PLACING) {
            if (mc.currentScreen instanceof ShulkerBoxScreen) {
                state = State.GEARING;
                openTicks = 0;
                return;
            }

            int invSlot = findAnyShulkerInInventory();
            if (invSlot == -1) {
                Services.CHAT.sendRaw("§cNo shulker found in inventory.");
                setEnabled(false);
                return;
            }

            BlockPos best = findBestPlacePos();
            if (best == null) {
                return;
            }

            ItemStack shulkerStack = mc.player.getInventory().getStack(invSlot);
            Item shulkerItem = shulkerStack.getItem();

            boolean success = placeBlocks(Collections.singletonList(best), shulkerItem);

            if (success) {
                placedPos = best;
                state = State.OPENING;
            }
            return;
        }

        if (state == State.OPENING) {
            if (mc.currentScreen instanceof ShulkerBoxScreen) {
                state = State.GEARING;
                openTicks = 0;
                return;
            }
            if (placedPos == null) {
                state = State.PLACING;
                return;
            }
            openPlacedShulker(placedPos);
            return;
        }

        if (state == State.GEARING) {
            if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
                state = State.OPENING;
                return;
            }

            if (openTicks < 3) {
                openTicks++;
                return;
            }

            boolean done = handleRegearInOpenShulker(true);
            if (done) {
                mc.currentScreen.close();
                state = State.DONE;
            }
        }
    }

    private boolean handleRegearInOpenShulker(boolean instant) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler == null || handler.slots.size() < 45) return false;
        if (!(mc.currentScreen instanceof ShulkerBoxScreen)) return false;

        if (!instant) {
            if (System.currentTimeMillis() - lastActionTime < speed.getValue().doubleValue()) return false;

            int actions = 0;
            List<Integer> clickSequence = buildClickSequence(handler);

            for (int slotId : clickSequence) {
                int rawSlot = Math.abs(slotId);
                SlotActionType action = (slotId > 0) ? SlotActionType.PICKUP : SlotActionType.QUICK_MOVE;
                mc.interactionManager.clickSlot(handler.syncId, rawSlot, 0, action, mc.player);
                actions++;
                if (actions >= clicks.getValue().intValue()) break;
            }

            lastActionTime = System.currentTimeMillis();

            if (actions == 0 && closeAfter.getValue()) {
                mc.currentScreen.close();
                return true;
            }
            return actions == 0;
        } else {
            int safety = 0;
            while (safety++ < 200) {
                List<Integer> clickSequence = buildClickSequence(handler);

                if (clickSequence.isEmpty()) return true;

                boolean didAny = false;
                for (int slotId : clickSequence) {
                    int rawSlot = Math.abs(slotId);
                    SlotActionType action = (slotId > 0) ? SlotActionType.PICKUP : SlotActionType.QUICK_MOVE;
                    mc.interactionManager.clickSlot(handler.syncId, rawSlot, 0, action, mc.player);
                    didAny = true;
                }

                if (!didAny) return true;
            }
            return false;
        }
    }

    private BlockPos findBestPlacePos() {
        if (mc.player == null || mc.world == null) return null;

        double range = placeRange.getValue().doubleValue();
        BlockPos origin = mc.player.getBlockPos();
        int r = (int) Math.ceil(range);

        BlockPos bestPos = null;
        double bestScore = -1;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);

                    if (!canPlace(pos)) continue;

                    double score = distanceToNearestOtherPlayerSq(pos);
                    if (score > bestScore) {
                        bestScore = score;
                        bestPos = pos;
                    }
                }
            }
        }
        return bestPos;
    }

    private void openPlacedShulker(BlockPos pos) {
        if (mc.player == null || mc.world == null) return;
        Vec3d hit = Vec3d.ofCenter(pos);
        BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public void setup() {
        Kit currentKit = Managers.KIT.getCurrentKit();
        if (currentKit == null) {
            Services.CHAT.sendRaw("§cNo kit is currently loaded.");
            expectedInv.clear();
            return;
        }
        expectedInv.clear();
        for (Map.Entry<Integer, KitItem> entry : currentKit.inventoryItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < 36) {
                expectedInv.put(entry.getKey(), entry.getValue());
            }
        }
        if (expectedInv.isEmpty()) {
            Services.CHAT.sendRaw("§cLoaded kit has no items in the main inventory.");
        }
    }

    private List<Integer> buildClickSequence(ScreenHandler handler) {
        List<Integer> clicks = new ArrayList<>();
        Map<Identifier, List<Integer>> availableContainerItems = new HashMap<>();

        int containerSize = handler.slots.size();
        boolean isShulker = containerSize == 63;
        int containerEnd = isShulker ? 27 : 54;






        for (int i = 0; i < containerEnd; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty()) {
                Identifier id = Registries.ITEM.getId(stack.getItem());
                availableContainerItems.computeIfAbsent(id, k -> new ArrayList<>()).add(i);
            }
        }

        for (int invIndex = 0; invIndex < 36; invIndex++) {
            KitItem requiredKitItem = expectedInv.get(invIndex);
            if (requiredKitItem == null) continue;

            int rawPlayerSlot = invIndexToHandlerSlot(handler, invIndex);
            ItemStack currentStack = handler.getSlot(rawPlayerSlot).getStack();

            Identifier requiredId = Registries.ITEM.getId(requiredKitItem.getItem());
            List<Integer> sourceSlots = availableContainerItems.get(requiredId);

            if (currentStack.getItem() == requiredKitItem.getItem()) {
                if (currentStack.isStackable() && currentStack.getCount() < requiredKitItem.count) {
                    if (sourceSlots != null && !sourceSlots.isEmpty()) {
                        int sourceSlot = sourceSlots.get(0);
                        clicks.add(sourceSlot);
                        clicks.add(rawPlayerSlot);
                        clicks.add(sourceSlot);
                        sourceSlots.remove(0);
                    }
                }
                continue;
            }

            if (sourceSlots != null && !sourceSlots.isEmpty()) {
                int sourceSlot = sourceSlots.get(0);
                if (currentStack.isEmpty()) {
                    clicks.add(sourceSlot);
                    clicks.add(rawPlayerSlot);
                } else {
                    clicks.add(sourceSlot);
                    clicks.add(rawPlayerSlot);
                    clicks.add(sourceSlot);
                }
                sourceSlots.remove(0);
            }
        }
        return clicks;
    }

    private int invIndexToHandlerSlot(ScreenHandler handler, int invIndex) {
        boolean isShulker = handler.slots.size() == 63;
        if (isShulker) {
            if (invIndex < 9) return 54 + invIndex;
            return 27 + (invIndex - 9);
        }
        int playerStart = 54;
        if (invIndex < 9) return playerStart + 27 + invIndex;
        return playerStart + (invIndex - 9);
    }

    private int findAnyShulkerInInventory() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (isShulkerStack(s)) return i;
        }
        return -1;
    }

    private boolean isShulkerStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return bi.getBlock() instanceof ShulkerBoxBlock;
    }

    private double distanceToNearestOtherPlayerSq(BlockPos pos) {
        if (mc.world == null || mc.player == null) return 0;
        Vec3d c = Vec3d.ofCenter(pos);
        double best = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == null || p == mc.player) continue;
            double d = p.squaredDistanceTo(c);
            if (d < best) best = d;
        }
        return best == Double.MAX_VALUE ? 999999 : best;
    }
}