package net.melbourne.settings.types;

import lombok.Getter;
import lombok.Setter;
import net.melbourne.Melbourne;
import net.melbourne.events.impl.SettingChangeEvent;
import net.melbourne.settings.Setting;

import java.util.function.Supplier;

@Getter
@Setter
public class NumberSetting extends Setting {
    private final Number defaultValue;
    private final Number minimum;
    private final Number maximum;
    private Number value;

    public NumberSetting(String name, String description, Number value, Number minimum, Number maximum) {
        super(name, description, true);
        this.value = value;
        this.defaultValue = value;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public NumberSetting(String name, String description, Number value, Number minimum, Number maximum, boolean visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public NumberSetting(String name, String description, Number value, Number minimum, Number maximum, Supplier<Boolean> visibility) {
        super(name, description, visibility);
        this.value = value;
        this.defaultValue = value;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public void setValue(Number value) {
        switch (getType()) {
            case LONG -> this.value = Math.clamp(value.longValue(), minimum.longValue(), maximum.longValue());
            case DOUBLE -> this.value = Math.clamp(value.doubleValue(), minimum.doubleValue(), maximum.doubleValue());
            case FLOAT -> this.value = Math.clamp(value.floatValue(), minimum.floatValue(), maximum.floatValue());
            default -> this.value = Math.clamp(value.intValue(), minimum.intValue(), maximum.intValue());
        }
        Melbourne.EVENT_HANDLER.post(new SettingChangeEvent(this));
    }

    public Type getType() {
        if (defaultValue.getClass() == Long.class) {
            return Type.LONG;
        } else if (defaultValue.getClass() == Double.class) {
            return Type.DOUBLE;
        } else if (defaultValue.getClass() == Float.class) {
            return Type.FLOAT;
        } else {
            return Type.INTEGER;
        }
    }

    public void resetValue() {
        value = defaultValue;
    }

    public enum Type {
        INTEGER, LONG, DOUBLE, FLOAT
    }
}