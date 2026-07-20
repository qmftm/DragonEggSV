package me.qmftm.dEench.game;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Applies information-hiding gamerules to every world:
 * <ul>
 *   <li>reducedDebugInfo — hides F3 coordinates.</li>
 *   <li>announceAdvancements — suppresses advancement broadcasts.</li>
 * </ul>
 */
public class WorldRulesService implements Listener {

    public void applyAll() {
        for (World world : Bukkit.getWorlds()) {
            apply(world);
        }
    }

    private void apply(World world) {
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        apply(event.getWorld());
    }
}
