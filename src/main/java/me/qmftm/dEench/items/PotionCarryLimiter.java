package me.qmftm.dEench.items;

import me.qmftm.dEench.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Restricts how many potions a player may carry (default 2). Blocks pickups
 * that would exceed the cap and drops any excess when an inventory is closed
 * (covers brewing stands, chests, etc.).
 */
public class PotionCarryLimiter implements Listener {

    private final PluginConfig config;

    public PotionCarryLimiter(PluginConfig config) {
        this.config = config;
    }

    private static boolean isPotion(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private int countPotions(Inventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && isPotion(item.getType())) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Item entity = event.getItem();
        if (!isPotion(entity.getItemStack().getType())) {
            return;
        }
        if (countPotions(player.getInventory()) >= config.maxPotions()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        int max = config.maxPotions();
        int excess = countPotions(player.getInventory()) - max;
        if (excess <= 0) {
            return;
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (excess <= 0) {
                break;
            }
            if (item == null || !isPotion(item.getType())) {
                continue;
            }
            int remove = Math.min(excess, item.getAmount());
            ItemStack dropped = item.clone();
            dropped.setAmount(remove);
            item.setAmount(item.getAmount() - remove);
            player.getWorld().dropItemNaturally(player.getLocation(), dropped);
            excess -= remove;
        }
    }
}
