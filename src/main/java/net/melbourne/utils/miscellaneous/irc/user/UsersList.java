package net.melbourne.utils.miscellaneous.irc.user;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UsersList {
    private final Map<UUID, String> users = new ConcurrentHashMap<>();

    public void addUser(UUID uuid, String name) {
        users.put(uuid, name);
    }

    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }

    public boolean contains(UUID uuid) {
        return users.containsKey(uuid);
    }

    public Map<UUID, String> getUsers() {
        return Collections.unmodifiableMap(users);
    }
}