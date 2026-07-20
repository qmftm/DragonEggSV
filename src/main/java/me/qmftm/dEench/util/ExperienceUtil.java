package me.qmftm.dEench.util;

import org.bukkit.entity.Player;

/**
 * Helpers for computing a player's exact experience points, since Bukkit only
 * exposes level + progress and its {@code getTotalExperience} is unreliable
 * after spending XP.
 */
public final class ExperienceUtil {

    private ExperienceUtil() {
    }

    /** XP points required to go from {@code level} to {@code level + 1}. */
    public static int expToNext(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    /** Total XP points a player currently holds (level + progress). */
    public static int getTotalExperience(Player player) {
        int level = player.getLevel();
        int total = Math.round(player.getExp() * expToNext(level));
        for (int i = 0; i < level; i++) {
            total += expToNext(i);
        }
        return total;
    }
}
