package net.melbourne.mixins;

import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.ReceiveChatEvent;
import net.melbourne.modules.impl.misc.BetterChatFeature;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Mixin(value = ChatHud.class, priority = 999)
public abstract class ChatHudMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow protected abstract boolean isChatHidden();
    @Shadow public abstract int getVisibleLineCount();
    @Shadow public abstract double getChatScale();
    @Shadow public abstract int getWidth();
    @Shadow protected abstract int getLineHeight();
    @Shadow private int scrolledLines;
    @Shadow private static double getMessageOpacityMultiplier(int age) { return 0; }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (signatureData == null) return;
        ReceiveChatEvent event = new ReceiveChatEvent(message, indicator, signatureData.hashCode(), false);
        Melbourne.EVENT_HANDLER.post(event);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite
    public void render(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused) {
        BetterChatFeature feature = Managers.FEATURE.getFeatureFromClass(BetterChatFeature.class);
        if (this.isChatHidden()) return;

        int visibleLineCount = this.getVisibleLineCount();
        if (this.visibleMessages.isEmpty()) return;

        float scale = (float) this.getChatScale();
        int chatWidth = MathHelper.ceil((float) this.getWidth() / scale);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(4.0F, 0.0F);

        int bottomY = MathHelper.floor((float) (context.getScaledWindowHeight() - 40) / scale);
        double bgOpacityPref = this.client.options.getTextBackgroundOpacity().getValue();
        int lineHeight = this.getLineHeight();

        float globalVerticalOffset = 0;
        String animMode = feature.isEnabled() ? feature.animMode.getValue() : "None";

        if (animMode.equals("Slide") && !this.visibleMessages.isEmpty()) {
            ChatHudLine.Visible newest = this.visibleMessages.get(0);
            if (feature.getAnimationMap().containsKey(newest)) {
                float f = feature.getAnimationMap().get(newest).get();
                globalVerticalOffset = (1.0f - f) * lineHeight;
            }
        }

        for (int i = 0; i + this.scrolledLines < this.visibleMessages.size() && i < visibleLineCount; ++i) {
            ChatHudLine.Visible line = this.visibleMessages.get(i + this.scrolledLines);
            if (line == null) continue;

            int age = currentTick - line.addedTime();
            if (age >= 200 && !focused) continue;

            context.getMatrices().pushMatrix();

            float yAnim = (animMode.equals("Slide") && i != 0) ? globalVerticalOffset : 0;
            context.getMatrices().translate(feature.offset.getValue().floatValue(), yAnim);

            double opacity = focused ? 1.0 : getMessageOpacityMultiplier(age);
            float animValue = 1.0f;

            if (feature.isEnabled() && !animMode.equals("None") && i == 0 && feature.getAnimationMap().containsKey(line)) {
                animValue = feature.getAnimationMap().get(line).get();
            }

            float alphaMultiplier = (feature.isEnabled() && animMode.equals("Fade") && i == 0) ? animValue : 1.0f;
            int baseAlpha = feature.isEnabled() ? feature.textAlpha.getValue().intValue() : 255;
            int textAlpha = (int) (baseAlpha * opacity * alphaMultiplier);

            if (textAlpha > 3) {
                int yPos = bottomY - i * lineHeight;
                int bgColor = (feature.isEnabled() && feature.background.getValue().equals("Custom")) ? feature.customColor.getColor().getRGB() : (feature.isEnabled() && feature.background.getValue().equals("Clear") ? 0 : ((int) (255.0 * opacity * bgOpacityPref) << 24));

                context.fill(-4, yPos - lineHeight, chatWidth + 8, yPos, bgColor);

                float animationX = 0;
                if (animMode.equals("Slide") && i == 0 && feature.getAnimationMap().containsKey(line)) {
                    animationX = (1.0f - animValue) * -chatWidth;
                }

                context.getMatrices().pushMatrix();
                context.getMatrices().translate(animationX, 0);
                context.drawTextWithShadow(this.client.textRenderer, line.content(), 0, yPos - lineHeight + 1, 0xFFFFFF + (textAlpha << 24));
                context.getMatrices().popMatrix();
            }
            context.getMatrices().popMatrix();
        }

        context.getMatrices().popMatrix();
    }

    @Redirect(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"))
    private void onAddVisibleMessage(List<ChatHudLine.Visible> instance, int index, Object element) {
        ChatHudLine.Visible visible = (ChatHudLine.Visible) element;
        BetterChatFeature feature = Managers.FEATURE.getFeatureFromClass(BetterChatFeature.class);

        if (feature.isEnabled() && !feature.animMode.getValue().equals("None") && index == 0) {
            feature.getAnimationMap().clear();
            feature.getAnimationMap().put(visible, new Animation(0f, 1f, feature.delay.getValue().intValue(), Easing.Method.EASE_OUT_QUART));
        }
        instance.add(index, visible);
    }

    @ModifyArgs(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHudLine;<init>(ILnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"))
    private void onModifyChatLine(Args args) {
        BetterChatFeature feature = Managers.FEATURE.getFeatureFromClass(BetterChatFeature.class);
        if (feature.isEnabled() && feature.timestamps.getValue()) {
            Text originalText = args.get(1);
            String time = new SimpleDateFormat("HH:mm").format(new Date());
            MutableText timestamp = Text.literal(feature.opening.getValue() + time + feature.closing.getValue() + " ").formatted(Formatting.GRAY);
            args.set(1, timestamp.append(originalText));
        }
    }
}