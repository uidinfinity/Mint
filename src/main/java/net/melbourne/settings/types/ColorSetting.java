package net.melbourne.settings.types;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Melbourne;
import net.melbourne.events.SubscribeEvent;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.settings.Setting;
import net.melbourne.utils.miscellaneous.ColorUtils;

import java.awt.*;
import java.util.function.Supplier;

@Getter
@Setter
public class ColorSetting extends Setting {
    private Color value;
    private final Color defaultValue;
    private boolean sync;

    public ColorSetting(String name, String description, Color color) {
        super(name, description, true);
        this.value = color;
        this.defaultValue = color;
        this.sync = false;
    }

    public ColorSetting(String name, String description, Color color, boolean visibility) {
        super(name, description, visibility);
        this.value = color;
        this.defaultValue = color;
        this.sync = false;
    }

    public ColorSetting(String name, String description, Color color, Supplier<Boolean> visibility) {
        super(name, description, visibility);
        this.value = color;
        this.defaultValue = color;
        this.sync = false;
    }

    @SubscribeEvent
    public Object getValue() {
        return getColor();
    }

    public Color getColor() {
        if (!sync) return value;
        Color g = ColorUtils.getGlobalColor();
        return new Color(g.getRed(), g.getGreen(), g.getBlue(), value.getAlpha());
    }

    public void setValue(Color v) {
        this.value = v;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public void setColor(Color v) {
        this.value = v;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public void resetValue() {
        this.value = defaultValue;
        this.sync = false;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public void setSync(boolean v) {
        this.sync = v;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }
}