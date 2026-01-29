package net.melbourne.settings.types;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.settings.Setting;

import java.util.function.Supplier;

@Getter
@Setter
public class TextSetting extends Setting {
    public String value;
    private final String defaultValue;

    public TextSetting(String name, String description, String value) {
        super(name, description, true);
        this.value = value;
        this.defaultValue = value;
    }

    public TextSetting(String name, String description, String value, boolean visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public TextSetting(String name, String description, String value, Supplier<Boolean> visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public void setValue(String value) {
        this.value = value;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public void resetValue() {
        value = defaultValue;
    }
}