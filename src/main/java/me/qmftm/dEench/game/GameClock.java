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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;

/**
 * Tracks elapsed MC days from an explicit start point ({@code /DE start}),
 * drives the date boss bar, and declares the winner on the final day. If no one
 * holds the egg on the final day, the win is deferred: the next player to
 * obtain the egg wins immediately.
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
        bossBar = BossBar.bossBar(Component.text("DragonEggSV"), 0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
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

    /**
     * Starts (or restarts) the game, counting from the given day number.
     */
    public void startGame(int startDay) {
        World overworld = Worlds.overworld();
        long full = overworld == null ? 0L : overworld.getFullTime();
        data.setStarted(true);
        data.setStartFullTime(full);
        data.setStartDay(startDay);
        data.setPaused(false);
        data.setPausedElapsed(0L);
        data.setWinDeclared(false);
        data.setWinDeferAnnounced(false);
        data.save();

        Bukkit.getServer().sendMessage(Component.text(
                "🥚 DragonEggSV has begun — Day " + startDay + "!", NamedTextColor.LIGHT_PURPLE));
    }

    /** Freezes the day counter. Returns false if not running or already paused. */
    public boolean pause() {
        if (!data.isStarted() || data.isPaused()) {
            return false;
        }
        data.setPausedElapsed(elapsedNow());
        data.setPaused(true);
        data.save();
        return true;
    }

    /** Resumes the day counter. Returns false if not paused. */
    public boolean resume() {
        if (!data.isStarted() || !data.isPaused()) {
            return false;
        }
        World overworld = Worlds.overworld();
        long full = overworld == null ? data.getStartFullTime() : overworld.getFullTime();
        data.setStartFullTime(full - data.getPausedElapsed());
        data.setPaused(false);
        data.save();
        return true;
    }

    public boolean isStarted() {
        return data.isStarted();
    }

    public boolean isPaused() {
        return data.isPaused();
    }

    public boolean isWinDeclared() {
        return data.isWinDeclared();
    }

    public int getWinDay() {
        return config.winDay();
    }

    public int currentDay() {
        if (!data.isStarted()) {
            return 0;
        }
        long elapsed = data.isPaused() ? data.getPausedElapsed() : elapsedNow();
        return (int) (elapsed / TICKS_PER_DAY) + data.getStartDay();
    }

    private long elapsedNow() {
        World overworld = Worlds.overworld();
        long full = overworld == null ? data.getStartFullTime() : overworld.getFullTime();
        return Math.max(0L, full - data.getStartFullTime());
    }

    private void tick() {
        if (!data.isStarted()) {
            showToAll(Component.text("DragonEggSV — /DE start 로 시작", NamedTextColor.GRAY), 0f);
            return;
        }

        boolean paused = data.isPaused();
        long elapsed = paused ? data.getPausedElapsed() : elapsedNow();
        int day = (int) (elapsed / TICKS_PER_DAY) + data.getStartDay();
        int winDay = config.winDay();
        float progress = clamp01((elapsed % TICKS_PER_DAY) / (float) TICKS_PER_DAY);

        Component title = Component.text(
                (paused ? "⏸ " : "🥚 ") + "Day " + day + " / " + winDay, NamedTextColor.LIGHT_PURPLE);
        showToAll(title, progress);

        if (!paused && !data.isWinDeclared() && day >= winDay) {
            resolveWin();
        }
    }

    private void resolveWin() {
        Player holder = eggManager.getHolder();
        if (holder != null) {
            data.setWinDeclared(true);
            data.save();
            Bukkit.getServer().sendMessage(Component.text(
                    "🐉 " + holder.getName() + " wins DragonEggSV — they hold the dragon egg!",
                    NamedTextColor.GOLD));
            winCeremony(holder);
        } else if (!data.isWinDeferAnnounced()) {
            data.setWinDeferAnnounced(true);
            data.save();
            Bukkit.getServer().sendMessage(Component.text(
                    "🐉 The final day has arrived, but no one holds the dragon egg. "
                            + "The next player to obtain it wins!",
                    NamedTextColor.GOLD));
        }
    }

    /** Teleport everyone to spawn and seat the winner on a victory dragon. */
    private void winCeremony(Player winner) {
        World overworld = Worlds.overworld();
        if (overworld == null) {
            return;
        }
        Location spawn = overworld.getSpawnLocation();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
        }

        EnderDragon dragon = overworld.spawn(spawn.clone().add(0, 5, 0), EnderDragon.class, d -> {
            d.setInvulnerable(true);
            d.setRemoveWhenFarAway(false);
            d.setPhase(EnderDragon.Phase.HOVER);
            d.customName(Component.text("👑 " + winner.getName(), NamedTextColor.GOLD));
            d.setCustomNameVisible(true);
        });
        dragon.addPassenger(winner);
    }

    private void showToAll(Component name, float progress) {
        bossBar.name(name);
        bossBar.progress(progress);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    private static float clamp01(float value) {
        return value < 0f ? 0f : Math.min(value, 1f);
    }
}
