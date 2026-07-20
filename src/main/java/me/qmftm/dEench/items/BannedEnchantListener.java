package me.qmftm.dEench.items;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import me.qmftm.dEench.DEench;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Prevents banned enchantments (mending, infinity) from being newly acquired
 * through the enchanting table, anvil, or villager trades, and strips them from
 * any items players already carry.
 */
public class BannedEnchantListener implements Listener {

    private final DEench plugin;
    private final Set<Enchantment> banned;

    public BannedEnchantListener(DEench plugin, Set<String> bannedNames) {
        this.plugin = plugin;
        this.banned = new java.util.HashSet<>();
        for (String name : bannedNames) {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (ench != null) {
                banned.add(ench);
            }
        }
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::sweep, 40L, 40L);
    }

    private void sweep() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                stripBanned(item);
            }
        }
    }

    private void stripBanned(ItemStack item) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        boolean changed = false;
        if (meta instanceof EnchantmentStorageMeta storage) {
            for (Enchantment ench : banned) {
                if (storage.hasStoredEnchant(ench)) {
                    storage.removeStoredEnchant(ench);
                    changed = true;
                }
            }
        } else {
            for (Enchantment ench : banned) {
                if (meta.hasEnchant(ench)) {
                    meta.removeEnchant(ench);
                    changed = true;
                }
            }
        }
        if (changed) {
            item.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        event.getEnchantsToAdd().keySet().removeIf(banned::contains);
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean changed = false;
        if (meta instanceof EnchantmentStorageMeta storage) {
            for (Enchantment ench : banned) {
                if (storage.hasStoredEnchant(ench)) {
                    storage.removeStoredEnchant(ench);
                    changed = true;
                }
            }
        } else {
            for (Enchantment ench : banned) {
                if (meta.hasEnchant(ench)) {
                    meta.removeEnchant(ench);
                    changed = true;
                }
            }
        }

        if (changed) {
            result.setItemMeta(meta);
            event.setResult(result);
        }
    }

    @EventHandler
    public void onTrade(VillagerAcquireTradeEvent event) {
        MerchantRecipe recipe = event.getRecipe();
        if (recipe == null) {
            return;
        }
        ItemStack result = recipe.getResult();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }

        Map<Enchantment, Integer> enchants = meta instanceof EnchantmentStorageMeta storage
                ? storage.getStoredEnchants()
                : result.getEnchantments();
        for (Iterator<Enchantment> it = enchants.keySet().iterator(); it.hasNext(); ) {
            if (banned.contains(it.next())) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
