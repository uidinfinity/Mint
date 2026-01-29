package net.melbourne.commands;

import lombok.AllArgsConstructor;

import java.awt.*;

@AllArgsConstructor
public enum State {
    Success(Color.GREEN),
    Info(Color.YELLOW),
    Warning(Color.RED);

    private final Color color;
}
