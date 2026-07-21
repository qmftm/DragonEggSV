package me.qmftm.dEench.egg;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents the dragon egg from being stored in chests or other container
 * blocks — it may only live in a player's inventory, as a placed block, or as
 * a dropped item.
 */
public class EggContainmentListener implements Listener {

    private static final Set<InventoryType> BLOCKED = EnumSet.of(
            InventoryType.CHEST,
            InventoryType.DISPENSER,
            InventoryType.DROPPER,
            InventoryType.HOPPER,
            InventoryType.BARREL,
            InventoryType.SHULKER_BOX,
            InventoryType.ENDER_CHEST,
            InventoryType.FURNACE,
            InventoryType.BLAST_FURNACE,
            InventoryType.SMOKER,
            InventoryType.BREWING);

    private static boolean isEgg(ItemStack item) {
        return item != null && item.getType() == Material.DRAGON_EGG;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!BLOCKED.contains(view.getTopInventory().getType())) {
            return;
        }
        boolean clickedTop = event.getClickedInventory() != null
                && event.getClickedInventory().equals(view.getTopInventory());
        boolean clickedBottom = event.getClickedInventory() != null
                && event.getClickedInventory().equals(view.getBottomInventory());

        // Placing an egg from the cursor into the container.
        if (clickedTop && isEgg(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        // Shift-clicking an egg from the player inventory into the container.
        if (clickedBottom && event.isShiftClick() && isEgg(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }
        // Number-key swap of a hotbar egg into a container slot.
        if (clickedTop && event.getClick() == ClickType.NUMBER_KEY && event.getWhoClicked() instanceof Player player) {
            if (isEgg(player.getInventory().getItem(event.getHotbarButton()))) {
                event.setCancelled(true);
                return;
            }
        }
        // Offhand swap of an egg into a container slot.
        if (clickedTop && event.getClick() == ClickType.SWAP_OFFHAND && event.getWhoClicked() instanceof Player player) {
            if (isEgg(player.getInventory().getItemInOffHand())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        if (!BLOCKED.contains(view.getTopInventory().getType()) || !isEgg(event.getOldCursor())) {
            return;
        }
        int topSize = view.getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Hoppers / minecarts must not vacuum up a dropped egg.
    @EventHandler
    public void onPickup(InventoryPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }
    }

    // Automation must not shuffle an egg between containers.
    @EventHandler
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getItem().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }
    }
}
