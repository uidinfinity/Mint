package net.melbourne.settings.types;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.interfaces.Nameable;
import net.melbourne.settings.Setting;

@Getter
@Setter
public class BindSetting extends Setting implements Nameable {
    public int value;
    private final int defaultValue;

    public BindSetting(String name, String description, int value) {
        super(name, description, true);
        this.value = value;
        this.defaultValue = value;
    }

    public BindSetting(String name, String description, int value, boolean visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public void setValue(int value) {
        this.value = value;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public void resetValue() {
        value = defaultValue;
    }

    @Override
    public String getName() {
        return name;
    }
}
