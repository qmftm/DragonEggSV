package me.qmftm.dEench.items;

import me.qmftm.dEench.DEench;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fully bans elytra, shulker boxes, ender chests, shields, totems of undying,
 * and tridents: they cannot be possessed, crafted, used, or obtained. The
 * ender pearl keeps its narrower rule (throwing is blocked, carrying is fine).
 */
public class BannedItemListener implements Listener {

    private final DEench plugin;

    public BannedItemListener(DEench plugin) {
        this.plugin = plugin;
    }

    /** True for items that are banned outright (possession/craft/use/obtain). */
    public static boolean isFullyBanned(Material material) {
        if (material == null) {
            return false;
        }
        if (Tag.SHULKER_BOXES.isTagged(material)) {
            return true;
        }
        return switch (material) {
            case ELYTRA, ENDER_CHEST, SHIELD, TOTEM_OF_UNDYING, TRIDENT -> true;
            default -> false;
        };
    }

    public void start() {
        // Sweep online players every second to strip newly obtained banned items.
        Bukkit.getScheduler().runTaskTimer(plugin, this::sweep, 20L, 20L);
    }

    private void sweep() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack[] contents = player.getInventory().getContents();
            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && isFullyBanned(item.getType())) {
                    player.getInventory().setItem(i, null);
                    changed = true;
                }
            }
            if (changed) {
                player.updateInventory();
            }
        }
    }

    // Obtain: block picking up banned items.
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && isFullyBanned(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
        }
    }

    // Craft: cancel any recipe that would produce a banned item.
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result != null && isFullyBanned(result.getType())) {
            event.getInventory().setResult(null);
        }
    }

    // Use: elytra gliding.
    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player && event.isGliding()) {
            event.setCancelled(true);
        }
    }

    // Use: totem of undying resurrection.
    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    // Ender pearl / trident: block the throw.
    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof EnderPearl
                || event.getEntity() instanceof org.bukkit.entity.Trident) {
            event.setCancelled(true);
        }
    }

    // Use: block right-click use of shields, tridents, and ender pearl throws.
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

    // Ender chest is caught by the fully-banned sweep, but also block opening one
    // that already exists in the world.
    @EventHandler
    public void onOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        InventoryType type = event.getInventory().getType();
        if (type == InventoryType.ENDER_CHEST || type == InventoryType.SHULKER_BOX) {
            event.setCancelled(true);
        }
    }
}
