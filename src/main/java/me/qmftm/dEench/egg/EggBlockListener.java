package me.qmftm.dEench.egg;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Keeps {@link EggManager}'s "placed egg" location in sync as the dragon egg
 * block is placed and broken, so the beacon-ambience cue can track it.
 */
public class EggBlockListener implements Listener {

    private final EggManager eggManager;

    public EggBlockListener(EggManager eggManager) {
        this.eggManager = eggManager;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            eggManager.setPlacedEgg(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            eggManager.clearPlacedEggAt(event.getBlock().getLocation());
        }
    }
}
