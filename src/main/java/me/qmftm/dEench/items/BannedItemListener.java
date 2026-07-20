package me.qmftm.dEench.items;

import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Bans usage of: elytra (gliding), ender pearls (throwing only), shulker boxes,
 * ender chests, shields, totems of undying, and tridents. Items may be held and
 * carried — only their use is blocked.
 */
public class BannedItemListener implements Listener {

    // Elytra: prevent gliding entirely.
    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player && event.isGliding()) {
            event.setCancelled(true);
        }
    }

    // Ender pearl / trident: block the throw.
    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof EnderPearl || event.getEntity() instanceof Trident) {
            event.setCancelled(true);
        }
    }

    // Totem of undying: prevent the resurrection pop.
    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    // Shulker box / ender chest: block opening.
    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.SHULKER_BOX || type == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
        }
    }

    // Shield / trident / ender pearl: block right-click use.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Material type = item.getType();
        if (type == Material.SHIELD || type == Material.TRIDENT || type == Material.ENDER_PEARL) {
            event.setCancelled(true);
        }
    }
}
