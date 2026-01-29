package net.melbourne.modules.impl.misc;

import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// stupid
@FeatureInfo(name = "AutoDupe", category = Category.Misc)
public class AutoDupeFeature extends Feature {

    public final ModeSetting mode =
            new ModeSetting("Mode", "", "Saddle", new String[]{"Saddle"});

    public final NumberSetting delay =
            new NumberSetting("Delay", "", 0.5, 0.0, 5.0);

    public final BooleanSetting serverLag =
            new BooleanSetting("PauseOnLag", "Pauses when server is lagging", true);

    private enum State {
        FIND_DONKEY,
        ENSURE_CHEST,
        CHECK_DONKEY_SNEAK,
        CHECK_DONKEY_OPEN,
        CHECK_DONKEY_CLEAR,
        MOUNT,
        WAIT_FOR_MOUNT,
        OPEN_INV,
        EQUIP_SADDLE,
        FILL_DONKEY,
        DROP_SADDLE,
        WAIT_AFTER_DROP,
        DISMOUNT,
        OPEN_CHEST,
        DEPOSIT,
        ENSURE_DISMOUNTED,
        RESET
    }

    private State state = State.FIND_DONKEY;
    private long nextAction = 0L;
    private long startTime = 0L;
    private int shulkersDuped = 0;

    private AbstractDonkeyEntity donkey;
    private BlockPos chestPos;

    private int depositIndex = 0;
    private final List<Integer> shulkerSlots = new ArrayList<>();
    private final List<BlockPos> filledChests = new ArrayList<>();
    private boolean isSneaking = false;

    @Override
    public void onEnable() {
        reset();
        startTime = System.currentTimeMillis();
        shulkersDuped = 0;
    }

    @Override
    public void onDisable() {
        sendStats();
        reset();
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        if (getNull()) return;

        if (serverLag.getValue() && Services.SERVER.getTimer().hasTimeElapsed(1000)) {
            return;
        }

        if (System.currentTimeMillis() < nextAction) return;

        switch (state) {
            case FIND_DONKEY -> findDonkey();
            case ENSURE_CHEST -> ensureChest();
            case CHECK_DONKEY_SNEAK -> checkDonkeySneak();
            case CHECK_DONKEY_OPEN -> checkDonkeyOpen();
            case CHECK_DONKEY_CLEAR -> checkDonkeyClear();
            case MOUNT -> mount();
            case WAIT_FOR_MOUNT -> waitForMount();
            case OPEN_INV -> openInv();
            case EQUIP_SADDLE -> equipSaddle();
            case FILL_DONKEY -> fillDonkey();
            case DROP_SADDLE -> dropSaddle();
            case WAIT_AFTER_DROP -> waitAfterDrop();
            case DISMOUNT -> dismount();
            case OPEN_CHEST -> openChest();
            case DEPOSIT -> deposit();
            case ENSURE_DISMOUNTED -> ensureDismounted();
            case RESET -> resetState();
        }
    }

    private void sendStats() {
        long duration = System.currentTimeMillis() - startTime;
        String uptime = formatTime(duration);
        int dubs = shulkersDuped / 54;

        String message = String.format("§a%d§r shulkers, §a%d§r dubs, uptime of §a%s§r.",
                shulkersDuped, dubs, uptime);

        Services.CHAT.sendRaw(message, true);
    }

    private String formatTime(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / (1000 * 60)) % 60;
        long hours = (ms / (1000 * 60 * 60));

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }

        if (minutes > 0) {
            if (sb.length() > 0) sb.append(seconds > 0 ? ", " : " and ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }

        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(" and ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }

    private void findDonkey() {
        donkey = mc.world.getEntitiesByClass(
                AbstractDonkeyEntity.class,
                mc.player.getBoundingBox().expand(8),
                Entity::isAlive
        ).stream().min(Comparator.comparingDouble(d -> d.squaredDistanceTo(mc.player))).orElse(null);

        state = donkey == null ? State.FIND_DONKEY : State.ENSURE_CHEST;
        waitDelay();
    }

    private void ensureChest() {
        if (donkey == null || !donkey.isAlive()) {
            state = State.FIND_DONKEY;
            return;
        }

        if (!donkey.hasChest()) {
            int slot = findHotbarItem(Items.CHEST);
            if (slot != -1) {
                mc.player.getInventory().setSelectedSlot(slot);
                mc.interactionManager.interactEntity(mc.player, donkey, Hand.MAIN_HAND);
                waitDelay();
                return;
            }
        }

        state = State.CHECK_DONKEY_SNEAK;
        waitDelay();
    }

    private void checkDonkeySneak() {
        if (donkey == null || !donkey.isAlive()) {
            state = State.RESET;
            return;
        }
        mc.options.sneakKey.setPressed(true);
        state = State.CHECK_DONKEY_OPEN;
        waitDelay();
    }

    private void checkDonkeyOpen() {
        if (donkey == null || !donkey.isAlive()) {
            mc.options.sneakKey.setPressed(false);
            state = State.RESET;
            return;
        }

        mc.interactionManager.interactEntity(mc.player, donkey, Hand.MAIN_HAND);
        state = State.CHECK_DONKEY_CLEAR;
        waitDelay();
    }

    private void checkDonkeyClear() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) {
            state = State.CHECK_DONKEY_OPEN;
            waitDelay();
            return;
        }

        ScreenHandler h = mc.player.currentScreenHandler;
        int chestEnd = h.slots.size() - 36;

        for (int i = 1; i < chestEnd; i++) {
            ItemStack s = h.getSlot(i).getStack();
            if (!s.isEmpty()) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                waitDelay();
                return;
            }
        }

        long extra = (long) (delay.getValue().doubleValue() * 1000.0);
        nextAction = System.currentTimeMillis() + extra;

        mc.player.closeHandledScreen();
        mc.options.sneakKey.setPressed(false);

        state = State.MOUNT;
    }

    private void mount() {
        if (donkey == null || !donkey.isAlive()) {
            state = State.RESET;
            return;
        }

        if (mc.options.sneakKey.isPressed()) {
            mc.options.sneakKey.setPressed(false);
        }

        mc.interactionManager.interactEntity(mc.player, donkey, Hand.MAIN_HAND);
        state = State.WAIT_FOR_MOUNT;
        waitDelay();
    }

    private void waitForMount() {
        if (mc.player.hasVehicle() && mc.player.getVehicle() == donkey) {
            state = State.OPEN_INV;
        } else {
            state = State.MOUNT;
        }
        waitDelay();
    }

    private void openInv() {
        if (!mc.player.hasVehicle()) {
            state = State.MOUNT;
            return;
        }

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY));
        state = State.EQUIP_SADDLE;
        waitDelay();
    }

    private void equipSaddle() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) {
            state = State.OPEN_INV;
            waitDelay();
            return;
        }

        ScreenHandler h = mc.player.currentScreenHandler;

        ItemStack saddleSlotStack = h.getSlot(0).getStack();
        if (saddleSlotStack.getItem() == Items.SADDLE) {
            state = State.FILL_DONKEY;
            waitDelay();
            return;
        }

        int saddleIndex = findContainerSlot(h, Items.SADDLE);
        if (saddleIndex == -1) {
            state = State.RESET;
            waitDelay();
            return;
        }

        mc.interactionManager.clickSlot(h.syncId, saddleIndex, 0, SlotActionType.QUICK_MOVE, mc.player);

        state = State.FILL_DONKEY;
        waitDelay();
    }

    private void fillDonkey() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) {
            state = State.OPEN_INV;
            waitDelay();
            return;
        }

        ScreenHandler h = mc.player.currentScreenHandler;
        int playerStart = h.slots.size() - 36;
        boolean moved = false;

        for (int i = playerStart; i < h.slots.size(); i++) {
            ItemStack s = h.getSlot(i).getStack();
            if (isShulker(s)) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                moved = true;
            }
        }

        if (moved) {
            waitDelay();
        }

        state = State.DROP_SADDLE;
    }

    private void dropSaddle() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) {
            state = State.RESET;
            return;
        }

        ScreenHandler h = mc.player.currentScreenHandler;

        mc.player.setPitch(90f);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), 90f, mc.player.isOnGround(), mc.player.isOnGround()));

        ItemStack s = h.getSlot(0).getStack();
        if (s.getItem() == Items.SADDLE) {
            mc.interactionManager.clickSlot(h.syncId, 0, 1, SlotActionType.THROW, mc.player);
        }

        state = State.WAIT_AFTER_DROP;
        nextAction = System.currentTimeMillis() + 250L;
    }

    private void waitAfterDrop() {
        state = State.DISMOUNT;
    }

    private void dismount() {
        if (mc.currentScreen != null) mc.currentScreen.close();
        mc.player.dismountVehicle();

        shulkerSlots.clear();
        depositIndex = 0;

        state = State.OPEN_CHEST;
        waitDelay();
    }

    private void openChest() {
        chestPos = findNearestChest();
        if (chestPos == null) {
            state = State.RESET;
            waitDelay();
            return;
        }

        Vec3d hit = Vec3d.ofCenter(chestPos);
        mc.interactionManager.interactBlock(
                mc.player,
                Hand.MAIN_HAND,
                new BlockHitResult(hit, Direction.UP, chestPos, false)
        );

        state = State.DEPOSIT;
        waitDelay();
    }

    private void deposit() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) {
            waitDelay();
            return;
        }

        ScreenHandler h = mc.player.currentScreenHandler;

        if (shulkerSlots.isEmpty()) {
            for (int i = 0; i < h.slots.size(); i++) {
                if (!isPlayerSlot(h, i)) continue;
                ItemStack s = h.getSlot(i).getStack();
                if (isShulker(s)) shulkerSlots.add(i);
            }
        }

        if (depositIndex >= shulkerSlots.size()) {
            mc.player.closeHandledScreen();
            sendStats();
            state = State.ENSURE_DISMOUNTED;
            isSneaking = false;
            waitDelay();
            return;
        }

        int target = findEmptyContainerSlot(h);

        if (target == -1) {
            mc.player.closeHandledScreen();
            if (chestPos != null) filledChests.add(chestPos);
            sendStats();
            shulkerSlots.clear();
            depositIndex = 0;
            state = State.OPEN_CHEST;
            waitDelay();
            return;
        }

        int slot = shulkerSlots.get(depositIndex++);
        mc.interactionManager.clickSlot(h.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(h.syncId, target, 0, SlotActionType.PICKUP, mc.player);

        shulkersDuped++;

        waitDelay();
    }

    private void ensureDismounted() {
        if (mc.currentScreen != null) mc.currentScreen.close();

        if (!isSneaking) {
            mc.options.sneakKey.setPressed(true);
            isSneaking = true;
            nextAction = System.currentTimeMillis() + 100L;
        } else {
            mc.options.sneakKey.setPressed(false);
            isSneaking = false;

            if (mc.player.hasVehicle()) {
                isSneaking = false;
                waitDelay();
            } else {
                state = State.RESET;
                waitDelay();
            }
        }
    }

    private void resetState() {
        if (mc.currentScreen != null) mc.currentScreen.close();
        if (mc.options.sneakKey.isPressed()) mc.options.sneakKey.setPressed(false);

        if (mc.player.hasVehicle()) {
            mc.player.dismountVehicle();
        }

        shulkerSlots.clear();
        filledChests.clear();
        depositIndex = 0;
        donkey = null;
        chestPos = null;
        isSneaking = false;
        state = State.FIND_DONKEY;
        waitDelay();
    }

    private void reset() {
        state = State.FIND_DONKEY;
        donkey = null;
        chestPos = null;
        shulkerSlots.clear();
        filledChests.clear();
        depositIndex = 0;
        isSneaking = false;
        nextAction = 0L;
        if (mc.options.sneakKey != null && mc.options.sneakKey.isPressed()) mc.options.sneakKey.setPressed(false);
    }

    private void waitDelay() {
        nextAction = System.currentTimeMillis() + (long) (delay.getValue().doubleValue() * 1000.0);
    }

    private int findHotbarItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private int findContainerSlot(ScreenHandler h, Item item) {
        int start = h.slots.size() - 36;
        for (int i = start; i < h.slots.size(); i++) {
            if (h.getSlot(i).getStack().getItem() == item) return i;
        }
        return -1;
    }

    private boolean isShulker(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        Identifier id = Registries.ITEM.getId(s.getItem());
        if (id == null) return false;
        String p = id.getPath();
        return p.endsWith("_shulker_box") || p.equals("shulker_box");
    }

    private boolean isPlayerSlot(ScreenHandler h, int slot) {
        return slot >= h.slots.size() - 36;
    }

    private int findEmptyContainerSlot(ScreenHandler h) {
        int end = h.slots.size() - 36;
        for (int i = 0; i < end; i++) {
            if (h.getSlot(i).getStack().isEmpty()) return i;
        }
        return -1;
    }

    private BlockPos findNearestChest() {
        BlockPos base = mc.player.getBlockPos();
        double best = Double.MAX_VALUE;
        BlockPos bestPos = null;

        for (BlockPos p : BlockPos.iterate(base.add(-6, -6, -6), base.add(6, 6, 6))) {
            if (filledChests.contains(p.toImmutable())) continue;

            BlockEntity be = mc.world.getBlockEntity(p);
            if (!(be instanceof ChestBlockEntity)) continue;
            double d = p.getSquaredDistance(base);
            if (d < best) {
                best = d;
                bestPos = p.toImmutable();
            }
        }
        return bestPos;
    }
}