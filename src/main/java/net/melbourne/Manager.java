package net.melbourne;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class Manager {
    public String name;
    public String description;

    public abstract void onInit();
}
