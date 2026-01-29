package net.melbourne.modules.impl.misc.announcer;

import net.melbourne.services.Services;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.BreakBlockEvent;
import net.melbourne.events.impl.PacketReceiveEvent;
import net.melbourne.events.impl.PacketSendEvent;
import net.melbourne.events.impl.PlayerUpdateEvent;
import net.melbourne.modules.Category;
import net.melbourne.modules.Feature;
import net.melbourne.modules.FeatureInfo;
import net.melbourne.modules.impl.misc.announcer.impl.TaskBasic;
import net.melbourne.modules.impl.misc.announcer.impl.TaskBlock;
import net.melbourne.modules.impl.misc.announcer.impl.TaskMove;
import net.melbourne.settings.types.BooleanSetting;
import net.melbourne.settings.types.ModeSetting;
import net.melbourne.settings.types.NumberSetting;
import net.melbourne.utils.miscellaneous.Timer;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;

import java.util.Iterator;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@FeatureInfo(name = "Announcer", category = Category.Misc)
public class AnnouncerFeature extends Feature {
    private final NumberSetting seconds = new NumberSetting("Seconds", "seconds in between announcing.", 5.0, 0.0, 60.0);
    private final ModeSetting mode = new ModeSetting("Mode", "How it will be broadcasted.", "Broadcast", new String[]{"Broadcast", "Clientside"});
    public BooleanSetting leave = new BooleanSetting("Leave", "Sends a message when someone leaves.", true);
    public BooleanSetting join = new BooleanSetting("Join", "Sends a message when someone joins.", true);
    public BooleanSetting mined = new BooleanSetting("Mined", "Sends a message when you mine a block.", true);
    public BooleanSetting placed = new BooleanSetting("Placed", "Sends a message when you place a block.", false);
    public BooleanSetting walk = new BooleanSetting("Walk", "Sends a message when you walk.", true);
    private final Queue<QueuedTask> toSend = new ConcurrentLinkedQueue<>();
    private final Timer timer = new Timer();

    @Override
    public String getInfo() {
        return mode.getValue();
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;

        if (timer.hasTimeElapsed(seconds.getValue().intValue() * 1000)) {
            timer.reset();

            if (toSend.isEmpty()) {
                return;
            }

            QueuedTask task = toSend.poll();
            String msg = task.getMessage();
            if (msg != null) {
                sendMessage(msg);
            }
        }

        if (walk.getValue()) {
            Iterator<QueuedTask> iterator = toSend.iterator();
            boolean hasMoveValue = false;

            while (iterator.hasNext()) {
                if (iterator.next() instanceof TaskMove) {
                    hasMoveValue = true;
                }
            }

            if (!hasMoveValue) {
                toSend.add(new TaskMove(TaskType.WALK));
            }
        }
    }


    @SubscribeEvent
    public void onBreakBlock(BreakBlockEvent event) {
        if (getNull()) return;

        if (mined.getValue()) {
            boolean found = false;
            Block block = mc.world.getBlockState(event.getPos()).getBlock();

            for (QueuedTask queued : toSend) {
                if (queued instanceof TaskBlock tb
                        && tb.type == TaskType.BREAK
                        && tb.block == block) {
                    tb.count++;
                    found = true;
                    break;
                }
            }

            if (!found)
                toSend.add(new TaskBlock(TaskType.BREAK, block));
        }
    }

    @SubscribeEvent
    public void onPacket(PacketSendEvent event) {
        if (getNull()) return;

        if (event.getPacket() instanceof PlayerInteractItemC2SPacket && mc.player.getMainHandStack().getItem() instanceof BlockItem item) {
            if (placed.getValue()) {
                boolean found = false;

                for (QueuedTask queued : toSend) {
                    if (queued instanceof TaskBlock tb
                            && tb.type == TaskType.PLACE
                            && tb.block == item.getBlock()) {

                        tb.count++;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    toSend.add(new TaskBlock(TaskType.PLACE, item.getBlock()));
                }
            }
        }
    }


    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (getNull()) return;

        if (event.getPacket() instanceof PlayerListS2CPacket packet && join.getValue()) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry list : packet.getPlayerAdditionEntries()) {
                    PlayerEntity player = mc.world.getPlayerByUuid(list.profileId());
                    QueuedTask task = new TaskBasic(TaskType.JOIN, MessagePrefixes.getMessage(TaskType.JOIN, player.getName().getString()));

                    if (timer.hasTimeElapsed(2000L)) {
                        timer.reset();
                        String msg = task.getMessage();

                        if (msg != null) {
                            sendMessage(msg);
                        }
                    }
                }
            }
        }

        if (event.getPacket() instanceof PlayerRemoveS2CPacket packet && leave.getValue()) {
            for (UUID uuid : packet.profileIds()) {
                PlayerEntity player = mc.world.getPlayerByUuid(uuid);
                QueuedTask task = new TaskBasic(TaskType.LEAVE, MessagePrefixes.getMessage(TaskType.LEAVE, player.getName().getString()));

                if (timer.hasTimeElapsed(2000L)) {
                    timer.reset();
                    String msg = task.getMessage();
                    if (msg != null) {
                        sendMessage(msg);
                    }
                }
            }
        }
    }

    private void sendMessage(String message) {
        if (mode.getValue().equalsIgnoreCase("Clientside")) {
            Services.CHAT.sendRaw(message);
        } else {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }
}
