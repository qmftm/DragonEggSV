package me.qmftm.dEench.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public double explosionDamageMultiplier() {
        return plugin.getConfig().getDouble("explosion-damage-multiplier", 0.25);
    }

    public int goldenAppleRegenSeconds() {
        return plugin.getConfig().getInt("golden-apple.regen-seconds", 2);
    }

    public int maxPotions() {
        return plugin.getConfig().getInt("potions.max-carry", 2);
    }

    public int dragonRespawnXp() {
        return plugin.getConfig().getInt("dragon-respawn-xp", 5000);
    }

    public Set<String> bannedEnchants() {
        List<String> list = plugin.getConfig().getStringList("banned-enchants");
        if (list.isEmpty()) {
            list = List.of("mending", "infinity");
        }
        return new LinkedHashSet<>(list);
    }
}
