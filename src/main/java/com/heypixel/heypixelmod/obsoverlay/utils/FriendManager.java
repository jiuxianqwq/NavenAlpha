package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendManager {
    private static final List<String> friends = new CopyOnWriteArrayList<>();

    public static boolean isFriend(Entity player) {
        return player instanceof Player && friends.contains(player.getName().getString());
    }

    public static boolean isFriend(String player) {
        return friends.contains(player);
    }

    public static void addFriend(Player player) {
        friends.add(player.getName().getString());
    }

    public static void addFriend(String name) {
        friends.add(name);
    }

    public static void removeFriend(Player player) {
        friends.remove(player.getName().getString());
    }

    public static List<String> getFriends() {
        return friends;
    }
}
