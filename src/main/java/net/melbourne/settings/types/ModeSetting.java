package net.melbourne.settings.types;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.interfaces.Nameable;
import net.melbourne.settings.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
public class ModeSetting extends Setting implements Nameable {
    private String value;
    private final String defaultValue;
    private final List<String> modes;

    public ModeSetting(String name, String description, String value, String[] modes) {
        super(name, description, true);
        this.value = value;
        this.defaultValue = value;
        this.modes = Arrays.asList(modes);
    }

    public ModeSetting(String name, String description, String value, String[] modes, boolean visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
        this.modes = Arrays.asList(modes);
    }

    public ModeSetting(String name, String description, String value, String[] modes, Supplier<Boolean> visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
        this.modes = Arrays.asList(modes);
    }

    public boolean equalsValue(String value) {
        return this.value.equalsIgnoreCase(value);
    }

    public void setValue(String value) {
        this.value = value;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public void resetValue() {
        this.value = defaultValue;
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    @Override
    public String getName() {
        return name;
    }
}