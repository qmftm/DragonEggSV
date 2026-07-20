package me.qmftm.dEench.game;

import me.qmftm.dEench.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Applies the configured per-dimension world border sizes.
 */
public class WorldBorderService {

    private final PluginConfig config;

    public WorldBorderService(PluginConfig config) {
        this.config = config;
    }

    public void applyAll() {
        for (World world : Bukkit.getWorlds()) {
            apply(world);
        }
    }

    public void apply(World world) {
        Location spawn = world.getSpawnLocation();
        world.getWorldBorder().setCenter(spawn.getX(), spawn.getZ());
        world.getWorldBorder().setSize(config.worldBorderSize(world.getEnvironment()));
    }
}
