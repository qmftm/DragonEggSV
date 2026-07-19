package me.qmftm.dEench;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

public final class DEench extends JavaPlugin {

    private final Map<Enchantment, Integer> overenchMax = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadOverench();

        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);

        PluginCommand command = getCommand("DE");
        if (command != null) {
            DECommand executor = new DECommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
    }

    /**
     * Custom maximum enchantment levels used by the anvil combine logic.
     */
    public Map<Enchantment, Integer> getOverenchMax() {
        return overenchMax;
    }

    /**
     * (Re)loads the {@code overench} section of config.yml into memory.
     */
    public void loadOverench() {
        overenchMax.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("overench");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Enchantment ench = resolveEnchantment(key);
            if (ench == null) {
                getLogger().warning("Unknown enchantment in config (overench." + key + "), skipping.");
                continue;
            }
            overenchMax.put(ench, section.getInt(key));
        }
    }

    /**
     * Updates a custom max level both in memory and in config.yml.
     */
    public void setOverench(Enchantment ench, int level) {
        overenchMax.put(ench, level);
        getConfig().set("overench." + configKey(ench), level);
        saveConfig();
    }

    private static String configKey(Enchantment ench) {
        NamespacedKey key = ench.getKey();
        return key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.asString();
    }

    /**
     * Resolves an enchantment from a name such as {@code sharpness} or
     * {@code minecraft:sharpness}. Returns {@code null} if it does not exist.
     */
    public static Enchantment resolveEnchantment(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.toLowerCase();
        NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        if (key == null) {
            return null;
        }
        return Registry.ENCHANTMENT.get(key);
    }
}
