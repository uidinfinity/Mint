package net.melbourne.services;

import net.melbourne.services.impl.*;

public class Services {
    public static BindService BIND;
    public static PlayerService PLAYER;
    public static BreakService BREAK;
    public static WorldService WORLD;
    public static ServerService SERVER;
    public static ChatService CHAT;
    public static SimulationService SIMULATION;
    public static InventoryService INVENTORY;
    public static RotationService ROTATION;
    public static HitboxService HITBOX;
    public static ShaderService SHADER;

    public void onInit() {
        BIND = new BindService();
        PLAYER = new PlayerService();
        BREAK = new BreakService();
        WORLD = new WorldService();
        SERVER = new ServerService();
        CHAT = new ChatService();
        SIMULATION = new SimulationService();
        INVENTORY = new InventoryService();
        ROTATION = new RotationService();
        HITBOX = new HitboxService();
    }

    public void onPostInit() {
        SHADER = new ShaderService();
    }
}
