package me.qmftm.dEench.egg;

import me.qmftm.dEench.DEench;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * Plays a beacon ambient sound at a placed dragon egg so nearby players can
 * locate it.
 */
public class BeaconAmbienceService {

    private final DEench plugin;
    private final EggManager eggManager;

    public BeaconAmbienceService(DEench plugin, EggManager eggManager) {
        this.plugin = plugin;
        this.eggManager = eggManager;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    private void tick() {
        Location loc = eggManager.getPlacedEgg();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        if (loc.getBlock().getType() != Material.DRAGON_EGG) {
            eggManager.clearPlacedEggAt(loc);
            return;
        }
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.0f);
    }
}
