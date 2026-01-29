package net.melbourne.settings;

import lombok.Getter;
import net.melbourne.interfaces.Nameable;
import java.util.function.Supplier;

@Getter
public abstract class Setting implements Nameable {

    public String name, description;
    public Supplier<Boolean> visibility;

    public Setting(String name, String description, boolean visibility) {
        this.name = name;
        this.description = description;
        this.visibility = () -> visibility;
    }

    public Setting(String name, String description, Supplier<Boolean> visibility) {
        this.name = name;
        this.description = description;
        this.visibility = visibility;
    }

    public boolean isVisible() {
        return visibility.get();
    }

    @Override
    public String getName() {
        return name;
    }
}