package net.melbourne.commands;

import lombok.Getter;
import net.melbourne.utils.Globals;

@Getter
public class Command implements Globals {

    public String name, description;

    public Command() {
        CommandInfo info = getClass().getAnnotation(CommandInfo.class);

        name = info.name();
        description = info.desc();
    }

    public void onCommand(String[] args) {
    }
}
