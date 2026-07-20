package me.qmftm.dEench.rules;

import me.qmftm.dEench.config.PluginConfig;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Reduces explosion damage from beds / respawn anchors / end crystals to a
 * fraction (default 1/4).
 */
public class ExplosionDamageListener implements Listener {

    private final PluginConfig config;

    public ExplosionDamageListener(PluginConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        DamageType type = source.getDamageType();

        boolean reduce = type == DamageType.BAD_RESPAWN_POINT; // beds & respawn anchors
        if (!reduce) {
            Entity direct = source.getDirectEntity();
            if (direct instanceof EnderCrystal) {
                reduce = true;
            }
        }

        if (reduce) {
            event.setDamage(event.getDamage() * config.explosionDamageMultiplier());
        }
    }
}
