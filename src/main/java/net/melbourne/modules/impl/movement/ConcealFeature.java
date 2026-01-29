package net.melbourne.modules.impl.movement;

import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.TickEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.PlaceFeature;
import net.melbourne.services.Services;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.inventory.switches.SearchLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@FeatureInfo(name = "Conceal", category = Category.Movement)
public class ConcealFeature extends PlaceFeature {

    private final NumberSetting offset = new NumberSetting("Offset", "Y offset after placing", 2.0, -5.0, 5.0);
    public final ModeSetting block = new ModeSetting("Block", "Block to burrow with", "Dynamic", new String[]{"Dynamic", "EnderChest", "Obsidian", "Piston", "Web"});
    private final BooleanSetting dynamic = new BooleanSetting("Dynamic", "Dynamic overlap positioning", true);

    private List<BlockPos> positions = new ArrayList<>();
    private long lastPlaceTime = 0L;
    private BlockPos initialPos = null;

    @Override
    public void onEnable() {
        if (getNull()) return;
        super.onEnable();
        positions.clear();
        lastPlaceTime = 0L;
        initialPos = getPlayerPos();

        if (dynamic.getValue()) {
            positions = getOverlapPos();
        } else {
            positions.add(getPlayerPos());
        }

        Item item = getPlaceItem();
        if (item == null) {
            setToggled(false);
            return;
        }

        for (BlockPos pos : positions) {
            if (!mc.world.getBlockState(pos).isReplaceable() && !intersectsWithEntity(pos)) {
                setToggled(false);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (getNull()) return;
        if (shouldDisable()) return;

        if (initialPos != null && !getPlayerPos().equals(initialPos)) {
            setToggled(false);
            return;
        }

        if (isMoving()) {
            setToggled(false);
            return;
        }

        if (!mc.player.isOnGround()) {
            lastPlaceTime = System.currentTimeMillis();
            return;
        }

        if (System.currentTimeMillis() - lastPlaceTime < 200) return;

        BlockPos pos = getPlayerPos();
        if (!mc.world.getBlockState(pos).isReplaceable()) {
            setToggled(false);
            return;
        }

        Item item = getPlaceItem();
        if (item == null) {
            setToggled(false);
            return;
        }

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.41999998688698, mc.player.getZ(), true, true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212, mc.player.getZ(), true, true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.00133597911214, mc.player.getZ(), true, true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16610926093821, mc.player.getZ(), true, true));

        if (placeBlocks(Collections.singletonList(pos), item)) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(),
                    mc.player.getY() + offset.getValue().doubleValue(),
                    mc.player.getZ(),
                    false, true));
            setToggled(false);
        }

        lastPlaceTime = System.currentTimeMillis();
    }

    private Item getPlaceItem() {
        String mode = block.getValue();

        if ("EnderChest".equals(mode)) return hasInHotbar(Items.ENDER_CHEST) ? Items.ENDER_CHEST : null;
        if ("Obsidian".equals(mode)) return hasInHotbar(Items.OBSIDIAN) ? Items.OBSIDIAN : null;
        if ("Piston".equals(mode)) return hasInHotbar(Items.PISTON) ? Items.PISTON : null;
        if ("Web".equals(mode)) return hasInHotbar(Items.COBWEB) ? Items.COBWEB : null;

        if ("Dynamic".equals(mode)) {
            if (hasInHotbar(Items.ENDER_CHEST)) return Items.ENDER_CHEST;
            if (hasInHotbar(Items.OBSIDIAN)) return Items.OBSIDIAN;
            if (hasInHotbar(Items.PISTON)) return Items.PISTON;
            if (hasInHotbar(Items.COBWEB)) return Items.COBWEB;
            return null;
        }

        return null;
    }

    private boolean hasInHotbar(Item item) {
        Slot slot = Services.INVENTORY.findSlot(SearchLogic.OnlyHotbar, item);
        return slot != null;
    }

    private boolean isMoving() {
        return mc.options.forwardKey.isPressed() ||
                mc.options.backKey.isPressed() ||
                mc.options.leftKey.isPressed() ||
                mc.options.rightKey.isPressed();
    }

    private BlockPos getPlayerPos() {
        double decimal = mc.player.getY() - Math.floor(mc.player.getY());
        double blockY = decimal > 0.8 ? Math.floor(mc.player.getY()) + 1 : Math.floor(mc.player.getY());
        return new BlockPos((int) Math.floor(mc.player.getX()), (int) blockY, (int) Math.floor(mc.player.getZ()));
    }

    private List<BlockPos> getOverlapPos() {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos base = getPlayerPos();
        double decX = Math.abs(mc.player.getX()) - Math.floor(Math.abs(mc.player.getX()));
        double decZ = Math.abs(mc.player.getZ()) - Math.floor(Math.abs(mc.player.getZ()));
        int offX = calcOffset(decX);
        int offZ = calcOffset(decZ);
        positions.add(base);
        for (int x = 0; x <= Math.abs(offX); x++) {
            for (int z = 0; z <= Math.abs(offZ); z++) {
                positions.add(base.add(x * offX, 0, z * offZ));
            }
        }
        return positions.stream().distinct().collect(Collectors.toList());
    }

    private int calcOffset(double dec) {
        return dec >= 0.7 ? 1 : (dec <= 0.3 ? -1 : 0);
    }

    private boolean intersectsWithEntity(BlockPos pos) {
        Box box = new Box(pos);
        return mc.world.getNonSpectatingEntities(Entity.class, box).stream()
                .anyMatch(e -> !e.equals(mc.player) && !(e instanceof ItemEntity));
    }
}
