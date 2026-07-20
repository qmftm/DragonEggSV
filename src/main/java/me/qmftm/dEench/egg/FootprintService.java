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
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Leaves a trail of {@code ^} text-display footprints beneath the egg holder,
 * each oriented toward the direction the holder is facing. Footprints last
 * 5 MC-days and are hidden at night.
 */
public class FootprintService {

    private static final String TAG = "de_footprint";
    private static final float VISIBLE_RANGE = 1.0f;
    private static final float HIDDEN_RANGE = 0.0f;

    private final DEench plugin;
    private final EggManager eggManager;
    private final PluginConfig config;
    private final List<Footprint> footprints = new ArrayList<>();

    public FootprintService(DEench plugin, EggManager eggManager, PluginConfig config) {
        this.plugin = plugin;
        this.eggManager = eggManager;
        this.config = config;
    }

    public void start() {
        // Clean up any footprints orphaned by a previous run/crash.
        removeTaggedEntities();

        long interval = Math.max(1L, config.footprintIntervalSeconds()) * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::stamp, interval, interval);
        Bukkit.getScheduler().runTaskTimer(plugin, this::maintain, 20L, 20L);
    }

    public void shutdown() {
        for (Footprint footprint : footprints) {
            if (footprint.display.isValid()) {
                footprint.display.remove();
            }
        }
        footprints.clear();
        removeTaggedEntities();
    }

    private void stamp() {
        Player holder = eggManager.getHolder();
        if (holder == null) {
            return;
        }

        Location at = holder.getLocation();
        World world = at.getWorld();
        float yaw = at.getYaw();
        Location loc = new Location(world, at.getX(), at.getY() + 0.05, at.getZ(), 0f, 0f);

        TextDisplay display = world.spawn(loc, TextDisplay.class, td -> {
            td.text(Component.text("^", NamedTextColor.DARK_PURPLE));
            td.setBillboard(Display.Billboard.FIXED);
            td.setViewRange(VISIBLE_RANGE);
            td.addScoreboardTag(TAG);
            // Lay the text flat on the ground (rotate about X) and turn the
            // caret to point where the holder is looking (rotate about Y).
            Quaternionf left = new Quaternionf()
                    .rotateY((float) Math.toRadians(-yaw))
                    .rotateX((float) Math.toRadians(90));
            td.setTransformation(new Transformation(
                    new Vector3f(), left, new Vector3f(1f, 1f, 1f), new Quaternionf()));
        });

        footprints.add(new Footprint(display, world.getFullTime()));
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

    private void removeTaggedEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(TAG)) {
                    entity.remove();
                }
            }
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
