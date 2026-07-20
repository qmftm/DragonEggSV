package me.qmftm.dEench.items;

import me.qmftm.dEench.config.PluginConfig;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Grants extra experience (default 5000) for killing a re-summoned ender
 * dragon. The first dragon (which drops the egg) keeps its vanilla reward.
 */
public class DragonRespawnXp implements Listener {

    private final PluginConfig config;

    public DragonRespawnXp(PluginConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        DragonBattle battle = dragon.getDragonBattle();
        if (battle != null && battle.hasBeenPreviouslyKilled()) {
            event.setDroppedExp(config.dragonRespawnXp());
        }
    }
}
