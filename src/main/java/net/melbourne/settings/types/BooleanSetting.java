package net.melbourne.settings.types;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.interfaces.Nameable;
import net.melbourne.settings.Setting;

import java.util.function.Supplier;

@Getter
@Setter
public class BooleanSetting extends Setting implements Nameable {
    public boolean value;
    private final boolean defaultValue;

    public BooleanSetting(String name, String description, boolean value) {
        super(name, description, true);
        this.value = value;
        this.defaultValue = value;
    }

    public BooleanSetting(String name, String description, boolean value, boolean visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public BooleanSetting(String name, String description, boolean value, Supplier<Boolean> visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
    }

    public boolean getValue() {
        return value;
    }

    public void resetValue() {
        value = defaultValue;
    }

    public void setValue(boolean value) {
        this.value = value;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    @Override
    public String getName() {
        return name;
    }
}