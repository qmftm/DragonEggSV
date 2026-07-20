package me.qmftm.dEench.egg;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * Prevents the egg holder from changing dimensions. The one exception is the
 * first acquirer travelling from the End to the Overworld (so the egg can be
 * carried out of the End after the dragon is killed).
 */
public class DimensionLockListener implements Listener {

    private final EggManager eggManager;

    public DimensionLockListener(EggManager eggManager) {
        this.eggManager = eggManager;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (!eggManager.isHolder(player)) {
            return;
        }

        if (event.getTo() != null
                && eggManager.isFirstHolder(player)
                && event.getFrom().getWorld().getEnvironment() == World.Environment.THE_END
                && event.getTo().getWorld().getEnvironment() == World.Environment.NORMAL) {
            return;
        }

        event.setCancelled(true);
    }
}
