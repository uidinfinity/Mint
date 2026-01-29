package net.melbourne.utils.entity.player.socials;

import net.melbourne.Manager;
import net.melbourne.Melbourne;
import net.melbourne.Managers;
import net.melbourne.modules.impl.client.EngineFeature;
import net.melbourne.utils.miscellaneous.FileUtils;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FriendManager extends Manager {

    private final List<String> friends = new ArrayList<>();
    private final String friendFile = Melbourne.NAME + "/client/friends.json";

    public FriendManager() {
        super("Friend", "Handles all friends");
    }

    @Override
    public void onInit() {
        loadFriends();
    }

    public void addFriend(String name) {
        if (!friends.contains(name)) {
            friends.add(name);
            saveFriends();
        }
    }

    public void removeFriend(String name) {
        if (friends.removeIf(f -> f.equalsIgnoreCase(name))) saveFriends();
    }

    public List<String> getFriends() {
        return friends;
    }

    public boolean isFriend(String name) {
        EngineFeature engine = Managers.FEATURE.getFeatureFromClass(EngineFeature.class);
        if (engine == null) return false;

        List<String> activeSocials = engine.socials.getWhitelistIds();

        if (activeSocials.contains("Teams")) {
            for (PlayerEntity player : MinecraftClient.getInstance().world.getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(name)) {
                    if (TeamUtils.isTeam(player)) return true;
                }
            }
        }

        if (activeSocials.contains("Friends")) {
            return friends.stream().anyMatch(f -> f.equalsIgnoreCase(name));
        }

        return false;
    }

    public void saveFriends() {
        try {
            FileUtils.createDirectory(Melbourne.NAME + "/client");
            JsonArray array = new JsonArray();
            for (String f : friends) array.add(f);
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(array, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFriends() {
        try {
            FileUtils.createDirectory(Melbourne.NAME + "/client");
            File file = new File(friendFile);
            if (!file.exists()) {
                file.createNewFile();
                saveFriends();
            }
            JsonArray array;
            try (Reader reader = new FileReader(file)) {
                array = JsonParser.parseReader(reader).getAsJsonArray();
            }
            friends.clear();
            for (JsonElement e : array) friends.add(e.getAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}