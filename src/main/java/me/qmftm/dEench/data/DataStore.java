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

    public boolean isStarted() {
        return cfg.getBoolean("game.started", false);
    }

    public void setStarted(boolean started) {
        cfg.set("game.started", started);
    }

    /** The day number the game is considered to begin at (default 1). */
    public int getStartDay() {
        return cfg.getInt("game.start-day", 1);
    }

    public void setStartDay(int day) {
        cfg.set("game.start-day", day);
    }

    public boolean isWinDeferAnnounced() {
        return cfg.getBoolean("game.win-defer-announced", false);
    }

    public void setWinDeferAnnounced(boolean announced) {
        cfg.set("game.win-defer-announced", announced);
    }

    public boolean isPaused() {
        return cfg.getBoolean("game.paused", false);
    }

    public void setPaused(boolean paused) {
        cfg.set("game.paused", paused);
    }

    /** Elapsed game ticks frozen at the moment of pausing. */
    public long getPausedElapsed() {
        return cfg.getLong("game.paused-elapsed", 0L);
    }

    public void setPausedElapsed(long elapsed) {
        cfg.set("game.paused-elapsed", elapsed);
    }
}
