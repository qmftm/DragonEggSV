package me.qmftm.dEench.config;

import me.qmftm.dEench.DEench;
import org.bukkit.World;

/**
 * Typed accessor over config.yml.
 */
public class PluginConfig {

    private final DEench plugin;

    public PluginConfig(DEench plugin) {
        this.plugin = plugin;
    }

    public int daysToWin() {
        return plugin.getConfig().getInt("game.days-to-win", 99);
    }

    /** Target day number that triggers the win (days-to-win + 1). */
    public int winDay() {
        return daysToWin() + 1;
    }

    public int worldBorderSize(World.Environment env) {
        return switch (env) {
            case NETHER -> plugin.getConfig().getInt("worldborder.nether", 500);
            case THE_END -> plugin.getConfig().getInt("worldborder.end", 400);
            default -> plugin.getConfig().getInt("worldborder.overworld", 1000);
        };
    }

    public int footprintDays() {
        return plugin.getConfig().getInt("egg.footprint-days", 5);
    }

    public int footprintIntervalSeconds() {
        return plugin.getConfig().getInt("egg.footprint-interval-seconds", 2);
    }

    public int altarBeaconMinutes() {
        return plugin.getConfig().getInt("egg.altar-beacon-minutes", 100);
    }

    public int villagerTradesPerDay() {
        return plugin.getConfig().getInt("egg.villager-trades-per-day", 20);
    }
}
