package me.qmftm.dEench.egg;

import me.qmftm.dEench.DEench;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Continuously emits enderman-style portal particles around the egg holder.
 */
public class EnderEffectService {

    private final DEench plugin;
    private final EggManager eggManager;

    public EnderEffectService(DEench plugin, EggManager eggManager) {
        this.plugin = plugin;
        this.eggManager = eggManager;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    private void tick() {
        Player holder = eggManager.getHolder();
        if (holder == null) {
            return;
        }
        Location center = holder.getLocation().add(0, 1.0, 0);
        holder.getWorld().spawnParticle(Particle.PORTAL, center, 12, 0.4, 0.8, 0.4, 0.05);
    }
}
