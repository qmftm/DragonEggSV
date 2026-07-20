package me.qmftm.dEench.egg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import me.qmftm.dEench.DEench;
import me.qmftm.dEench.config.PluginConfig;
import me.qmftm.dEench.util.Worlds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Leaves a trail of {@code ^} text-display footprints beneath the egg holder,
 * each oriented toward the direction the holder is facing. Footprints last
 * 5 MC-days and are hidden at night.
 *
 * <p>Footprints are persisted as real display entities: each stores its spawn
 * time in its PDC, and on startup any surviving footprints are re-adopted so
 * their lifetime carries across restarts.
 */
public class FootprintService {

    private static final String TAG = "de_footprint";
    private static final float VISIBLE_RANGE = 1.0f;
    private static final float HIDDEN_RANGE = 0.0f;

    private final DEench plugin;
    private final EggManager eggManager;
    private final PluginConfig config;
    private final NamespacedKey spawnKey;
    private final List<Footprint> footprints = new ArrayList<>();

    public FootprintService(DEench plugin, EggManager eggManager, PluginConfig config) {
        this.plugin = plugin;
        this.eggManager = eggManager;
        this.config = config;
        this.spawnKey = new NamespacedKey(plugin, "footprint_spawn");
    }

    public void start() {
        adoptExisting();

        long interval = Math.max(1L, config.footprintIntervalSeconds()) * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::stamp, interval, interval);
        Bukkit.getScheduler().runTaskTimer(plugin, this::maintain, 20L, 20L);
    }

    public void shutdown() {
        // Leave the display entities in the world; they persist and are
        // re-adopted on the next start.
        footprints.clear();
    }

    /** Re-adopt footprints saved in loaded chunks from a previous run. */
    private void adoptExisting() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof TextDisplay display && display.getScoreboardTags().contains(TAG)) {
                    Long spawn = display.getPersistentDataContainer().get(spawnKey, PersistentDataType.LONG);
                    footprints.add(new Footprint(display, spawn != null ? spawn : world.getFullTime()));
                }
            }
        }
    }

    private void stamp() {
        Player holder = eggManager.getHolder();
        if (holder == null) {
            return;
        }

        Location at = holder.getLocation();
        World world = at.getWorld();
        float yaw = at.getYaw();
        long spawnFullTime = world.getFullTime();
        Location loc = new Location(world, at.getX(), at.getY() + 0.05, at.getZ(), 0f, 0f);

        TextDisplay display = world.spawn(loc, TextDisplay.class, td -> {
            td.text(Component.text("^", NamedTextColor.DARK_PURPLE));
            td.setBillboard(Display.Billboard.FIXED);
            td.setViewRange(VISIBLE_RANGE);
            td.addScoreboardTag(TAG);
            td.setPersistent(true);
            td.getPersistentDataContainer().set(spawnKey, PersistentDataType.LONG, spawnFullTime);
            // Lay the text flat on the ground (rotate about X) and turn the
            // caret to point where the holder is looking (rotate about Y).
            Quaternionf left = new Quaternionf()
                    .rotateY((float) Math.toRadians(-yaw))
                    .rotateX((float) Math.toRadians(90));
            td.setTransformation(new Transformation(
                    new Vector3f(), left, new Vector3f(1f, 1f, 1f), new Quaternionf()));
        });

        footprints.add(new Footprint(display, spawnFullTime));
    }

    private void maintain() {
        long maxAge = (long) config.footprintDays() * 24000L;
        Iterator<Footprint> it = footprints.iterator();
        while (it.hasNext()) {
            Footprint footprint = it.next();
            TextDisplay display = footprint.display;
            if (!display.isValid()) {
                it.remove();
                continue;
            }
            World world = display.getWorld();
            if (world.getFullTime() - footprint.spawnFullTime > maxAge) {
                display.remove();
                it.remove();
                continue;
            }
            display.setViewRange(Worlds.isNight(world) ? HIDDEN_RANGE : VISIBLE_RANGE);
        }
    }

    private static final class Footprint {
        private final TextDisplay display;
        private final long spawnFullTime;

        private Footprint(TextDisplay display, long spawnFullTime) {
            this.display = display;
            this.spawnFullTime = spawnFullTime;
        }
    }
}
