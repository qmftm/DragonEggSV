package me.qmftm.dEench.data;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import me.qmftm.dEench.DEench;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Persistent game/egg state stored in data.yml, surviving restarts of the
 * long-running (99 MC-day) server.
 */
public class DataStore {

    private final DEench plugin;
    private final File file;
    private FileConfiguration cfg;

    public DataStore(DEench plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (cfg == null) {
            return;
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data.yml: " + e.getMessage());
        }
    }

    /** Overworld fullTime captured when the game started; -1 if not yet set. */
    public long getStartFullTime() {
        return cfg.getLong("game.start-fulltime", -1L);
    }

    public void setStartFullTime(long fullTime) {
        cfg.set("game.start-fulltime", fullTime);
    }

    public UUID getFirstHolder() {
        String raw = cfg.getString("egg.first-holder");
        return raw == null ? null : UUID.fromString(raw);
    }

    public void setFirstHolder(UUID uuid) {
        cfg.set("egg.first-holder", uuid == null ? null : uuid.toString());
    }

    public boolean isWinDeclared() {
        return cfg.getBoolean("game.win-declared", false);
    }

    public void setWinDeclared(boolean declared) {
        cfg.set("game.win-declared", declared);
    }
}
