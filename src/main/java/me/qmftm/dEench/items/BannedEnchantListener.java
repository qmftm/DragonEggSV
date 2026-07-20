package me.qmftm.dEench.items;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
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
 * through the enchanting table, anvil, or villager trades. Existing items are
 * left untouched.
 */
public class BannedEnchantListener implements Listener {

    private final Set<Enchantment> banned;

    public BannedEnchantListener(Set<String> bannedNames) {
        this.banned = new java.util.HashSet<>();
        for (String name : bannedNames) {
            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (ench != null) {
                banned.add(ench);
            }
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
