package net.melbourne.services.impl;

import net.melbourne.Melbourne;
import net.melbourne.services.Service;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.melbourne.mixins.accessors.ChatHudAccessor;

import static net.melbourne.utils.Globals.mc;

public class ChatService extends Service {
    public final Map<String, MessageSignatureData> persistentMessages = new HashMap<>();
    public MessageSignatureData transientSignature = null;
    private static final String POP_PREFIX = "pop:";
    private static final String MODULE_PREFIX = "module:";
    private static final String MACRO_PREFIX = "macro:";

    public ChatService() {
        super("Chat", "Handles the creation of chat messages.");
        Melbourne.EVENT_HANDLER.subscribe(this);
    }

    private MessageSignatureData generateSignature() {
        byte[] data = new byte[256];
        new SecureRandom().nextBytes(data);
        return new MessageSignatureData(data);
    }

    public void sendRaw(String message) {
        sendRaw(Text.literal(message), true);
    }

    public void sendRaw(String message, boolean prefix) {
        sendRaw(Text.literal(message), prefix);
    }

    public void sendRaw(Text message) {
        sendRaw(message, true);
    }

    public void sendRaw(Text message, boolean prefix) {
        if (mc == null || mc.inGameHud == null || getChatHud() == null) return;
        Text text = prefix ? prefix().copy().append(message) : message;
        getChatHud().addMessage(text);
    }

    public void sendModuleToggle(String moduleName, Text message, boolean prefix) {
        String key = MODULE_PREFIX + moduleName;
        sendPersistent(key, message, prefix);
    }

    public void sendMacroToggle(String macroName, Text message, boolean prefix) {
        String key = MACRO_PREFIX + macroName;
        sendPersistent(key, message, prefix);
    }

    public void sendPop(UUID playerUuid, Text message, boolean prefix) {
        String key = POP_PREFIX + playerUuid.toString();
        sendPersistent(key, message, prefix);
    }

    public void sendPersistent(String key, Text message, boolean prefix) {
        if (mc == null || mc.inGameHud == null || getChatHud() == null) return;
        ChatHud chatHud = getChatHud();
        if (persistentMessages.containsKey(key)) {
            removeSilently(persistentMessages.get(key));
        }
        Text text = prefix ? prefix().copy().append(message) : message;
        MessageSignatureData signature = generateSignature();
        MessageIndicator indicator = indicator();
        chatHud.addMessage(text, signature, indicator);
        persistentMessages.put(key, signature);
    }

    public void removePersistent(String key) {
        if (mc == null || mc.inGameHud == null || getChatHud() == null) return;
        if (persistentMessages.containsKey(key)) {
            removeSilently(persistentMessages.get(key));
            persistentMessages.remove(key);
        }
    }

    public void removeSilently(MessageSignatureData signature) {
        if (mc == null || mc.inGameHud == null || getChatHud() == null) return;
        ChatHud hud = getChatHud();
        ChatHudAccessor accessor = (ChatHudAccessor) hud;
        accessor.getMessages().removeIf(line -> signature.equals(line.signature()));
        accessor.getVisibleMessages().removeIf(visible -> false);
        accessor.callRefresh();
    }

    private ChatHud getChatHud() {
        return mc.inGameHud.getChatHud();
    }

    private MessageIndicator indicator() {
        int global = ColorUtils.getGlobalColor().getRGB() & 0x00FFFFFF;
        return new MessageIndicator(global, null, Text.literal("Mint"), "Mint");
    }

    private Text prefix() {
        Text melbourne = Text.literal("§7[§sMint§7] §r");
        return Text.empty().append(melbourne);
    }
}
