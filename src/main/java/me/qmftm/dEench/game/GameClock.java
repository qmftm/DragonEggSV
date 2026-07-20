package me.qmftm.dEench.game;

import me.qmftm.dEench.DEench;
import me.qmftm.dEench.config.PluginConfig;
import me.qmftm.dEench.data.DataStore;
import me.qmftm.dEench.egg.EggManager;
import me.qmftm.dEench.util.Worlds;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Tracks elapsed MC days from a fixed start point, drives the date boss bar,
 * and declares the winner once the target day is reached.
 */
public class GameClock {

    private static final long TICKS_PER_DAY = 24000L;

    private final DEench plugin;
    private final DataStore data;
    private final PluginConfig config;
    private final EggManager eggManager;

    private BossBar bossBar;

    public GameClock(DEench plugin, DataStore data, PluginConfig config, EggManager eggManager) {
        this.plugin = plugin;
        this.data = data;
        this.config = config;
        this.eggManager = eggManager;
    }

    public void start() {
        bossBar = BossBar.bossBar(Component.text("Day 1"), 0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (bossBar == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
    }

    private void tick() {
        World overworld = Worlds.overworld();
        if (overworld == null) {
            return;
        }

        long full = overworld.getFullTime();
        long start = data.getStartFullTime();
        if (start < 0) {
            start = full;
            data.setStartFullTime(start);
            data.save();
        }

        long elapsed = Math.max(0L, full - start);
        int day = (int) (elapsed / TICKS_PER_DAY) + 1;
        int winDay = config.winDay();
        float progress = clamp01((elapsed % TICKS_PER_DAY) / (float) TICKS_PER_DAY);

        bossBar.name(Component.text("🥚 Day " + day + " / " + winDay, NamedTextColor.LIGHT_PURPLE));
        bossBar.progress(progress);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }

        if (day >= winDay && !data.isWinDeclared()) {
            declareWin();
        }
    }

    private void declareWin() {
        data.setWinDeclared(true);
        data.save();

        Player holder = eggManager.getHolder();
        Component message;
        if (holder != null) {
            message = Component.text("🐉 " + holder.getName() + " wins DragonEggSV — they hold the dragon egg on the final day!",
                    NamedTextColor.GOLD);
        } else {
            message = Component.text("🐉 The final day has arrived, but no player is currently holding the dragon egg.",
                    NamedTextColor.GOLD);
        }
        Bukkit.getServer().sendMessage(message);
    }

    private static float clamp01(float value) {
        return value < 0f ? 0f : Math.min(value, 1f);
    }
}
