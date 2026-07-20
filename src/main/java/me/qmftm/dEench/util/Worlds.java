package me.qmftm.dEench.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

public final class Worlds {

    private Worlds() {
    }

    /**
     * The overworld used as the authoritative clock/border reference.
     */
    public static World overworld() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                return world;
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    /**
     * True when the given world's time of day is night (footprints hide at night).
     */
    public static boolean isNight(World world) {
        long time = world.getTime();
        return time >= 13000L && time <= 23000L;
    }
}
