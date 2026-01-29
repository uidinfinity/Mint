package net.melbourne.modules;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Managers;
import net.melbourne.Melbourne;
import net.melbourne.services.Services;
import net.melbourne.modules.impl.client.NotificationsFeature;
import net.melbourne.modules.impl.misc.RoboticsFeature;
import net.melbourne.settings.Setting;
import net.melbourne.settings.types.*;
import net.melbourne.utils.Globals;
import net.melbourne.utils.animations.Animation;
import net.melbourne.utils.animations.Easing;
import net.melbourne.utils.miscellaneous.ColorUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Feature implements Globals {
    public String name, desc;
    public Category category;
    private boolean enabled;
    private final List<Setting> settings = new ArrayList<>();
    public final ModeSetting hiddenMode;
    public final BindSetting bind;
    public final ModeSetting bindMode;
    private boolean newEntry;

    private final Animation animationX;
    private final Animation animationY;

    public Feature() {
        FeatureInfo info = getClass().getAnnotation(FeatureInfo.class);

        int defaultBind = InputUtil.UNKNOWN_KEY.getCode();

        if (info != null) {
            name = info.name();
            desc = info.desc();
            category = info.category();
            defaultBind = info.bind();
        }

        hiddenMode = new ModeSetting("Hidden", "Changes if it gets displayed in the modulelist.", "Visible", new String[]{"Visible", "Hidden", "Info"});
        bind = new BindSetting("Bind", "Allows you to bind the module.", defaultBind);
        bindMode = new ModeSetting("Toggle", "Changes the bind mode of the module.", "Toggle", new String[]{"Toggle", "Hold"});

        animationX = new Animation(300, Easing.Method.EASE_OUT_CUBIC);
        animationY = new Animation(300, Easing.Method.EASE_OUT_CUBIC);
        enabled = false;
    }

    public void onEnable() {}
    public void onDisable() {}
    public String getInfo() { return ""; }
    public boolean getNull() { return mc.world == null || mc.player == null; }

    public void setEnabled(boolean enabled) {
        setEnabled(enabled, true);
    }

    public void setEnabled(boolean enabled, boolean notification) {
        boolean wasEnabled = this.enabled;
        this.enabled = enabled;

        Feature robotics = Managers.FEATURE.getFeatureByName("Robotics");
        if (robotics instanceof RoboticsFeature rf) {
            rf.syncToggle(this, enabled);
        }

        if (enabled && !wasEnabled) {
            newEntry = true;
            onEnable();
            Melbourne.EVENT_HANDLER.subscribe(this);
            if (notification && !getNull()) sendEnableNotification();
        } else if (!enabled && wasEnabled) {
            onDisable();
            Melbourne.EVENT_HANDLER.unsubscribe(this);
            if (notification && !getNull()) sendDisableNotification();
        }
    }

    public void setToggled(boolean enabled) {
        this.setEnabled(enabled, false);
        NotificationsFeature notifications = getNotifications();
        Text text = Text.literal("")
                .append(Text.literal(name).styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB())))
                .append(Text.literal(" §7has been "))
                .append(Text.literal("toggled").styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB())))
                .append(Text.literal("§7."));
        Services.CHAT.sendModuleToggle(name, text, true);
    }

    private void sendEnableNotification() {
        NotificationsFeature notifications = getNotifications();
        if (notifications != null && notifications.moduleToggles.getValue()) {
            Text txt = Text.literal("")
                    .append(Text.literal(name).styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB())))
                    .append(Text.literal(" §7has been "))
                    .append(Text.literal("enabled").styled(s -> s.withColor(Formatting.GREEN)))
                    .append(Text.literal("§7."));
            Services.CHAT.sendModuleToggle(name, txt, true);
        }
    }

    private void sendDisableNotification() {
        NotificationsFeature notifications = getNotifications();
        if (notifications == null || !notifications.moduleToggles.getValue()) return;
        Text txt = Text.literal("")
                .append(Text.literal(name).styled(s -> s.withColor(ColorUtils.getGlobalColor().getRGB())))
                .append(Text.literal(" §7has been "))
                .append(Text.literal("disabled").styled(s -> s.withColor(Formatting.RED)))
                .append(Text.literal("§7."));
        Services.CHAT.sendModuleToggle(name, txt, true);
    }

    private NotificationsFeature getNotifications() {
        return (NotificationsFeature) Managers.FEATURE.getFeatureByName("Notifications");
    }

    public void resetValues() {
        for (Setting s : settings) {
            if (s instanceof BooleanSetting b) b.resetValue();
            else if (s instanceof NumberSetting n) n.resetValue();
            else if (s instanceof ModeSetting m) m.resetValue();
            else if (s instanceof TextSetting t) t.resetValue();
            else if (s instanceof BindSetting bn) bn.resetValue();
            else if (s instanceof ColorSetting c) c.resetValue();
        }
    }
}