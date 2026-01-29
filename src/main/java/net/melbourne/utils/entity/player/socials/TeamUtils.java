package net.melbourne.utils.entity.player.socials;

import net.melbourne.utils.Globals;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TextColor;

public class TeamUtils implements Globals {

    public static boolean isEnemy(PlayerEntity player) {
        TextColor enemyColor = player.getDisplayName().getStyle().getColor();
        TextColor myColor = mc.player.getDisplayName().getStyle().getColor();

        if (enemyColor == null || myColor == null) return true;

        return !enemyColor.equals(myColor);
    }

    public static boolean isTeam(PlayerEntity player) {
        TextColor otherColor = player.getDisplayName().getStyle().getColor();
        TextColor myColor = mc.player.getDisplayName().getStyle().getColor();

        if (otherColor == null || myColor == null) return false;

        return otherColor.equals(myColor);
    }
}