package me.qmftm.dEench.rules;

import me.qmftm.dEench.util.Worlds;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Enforces two end-related rules:
 * <ul>
 *   <li>No end credits/poem: the End exit portal is handled manually so the
 *       credits sequence never plays.</li>
 *   <li>The outer-island end gateway is disabled (teleport cancelled).</li>
 * </ul>
 */
public class EndPortalListener implements Listener {

    /** Handle the End exit portal ourselves so the credits never trigger. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerPortalEvent.TeleportCause.END_PORTAL) {
            return;
        }
        if (event.getFrom().getWorld() == null
                || event.getFrom().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }

        Player player = event.getPlayer();
        World overworld = Worlds.overworld();
        if (overworld == null) {
            return;
        }
        Location target = player.getRespawnLocation();
        if (target == null) {
            target = overworld.getSpawnLocation();
        }

        event.setCancelled(true);
        player.teleport(target);
    }

    /** Disable the end gateway to the outer islands. */
    @EventHandler(ignoreCancelled = true)
    public void onGateway(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            event.setCancelled(true);
        }
    }
}
