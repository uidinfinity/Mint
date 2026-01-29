package net.melbourne.modules;

import lombok.AllArgsConstructor;
import net.melbourne.Managers;
import net.melbourne.interfaces.Nameable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public enum Category implements Nameable {
    Combat("Combat"),
    Misc("Misc"),
    Render("Render"),
    Movement("Movement"),
    Player("Player"),
    Legit("Legit"),
    Client("Client"),
    Scripts("Scripts");

    private final String name;

    @Override
    public String getName() {
        return name;
    }
}