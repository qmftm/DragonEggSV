package me.qmftm.dEench;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class AnvilListener implements Listener {

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final DEench plugin;

    public AnvilListener(DEench plugin) {
        this.plugin = plugin;
    }

    private Map<Enchantment, Integer> customMax() {
        return plugin.getOverenchMax();
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        Map<Enchantment, Integer> customMax = customMax();
        AnvilInventory anvil = event.getInventory();
        ItemStack left = anvil.getItem(0);
        ItemStack right = anvil.getItem(1);
        if (left == null || right == null) {
            return;
        }

        Map<Enchantment, Integer> rightEnchs = getEnchants(right);
        if (rightEnchs.isEmpty()) {
            return;
        }

        boolean leftIsBook = left.getType() == Material.ENCHANTED_BOOK;

        // If any applicable enchantment on the left item is already at the custom
        // maximum, cancel the combine entirely.
        for (Map.Entry<Enchantment, Integer> entry : rightEnchs.entrySet()) {
            Enchantment ench = entry.getKey();
            if (!customMax.containsKey(ench)) {
                continue;
            }
            if (!leftIsBook && !ench.canEnchantItem(left)) {
                continue;
            }
            if (getEnchantLevel(left, ench) < customMax.get(ench)) {
                continue;
            }
            event.setResult(null);
            return;
        }

        ItemStack result = event.getResult() != null ? event.getResult().clone() : left.clone();
        boolean modified = false;

        for (Map.Entry<Enchantment, Integer> entry : rightEnchs.entrySet()) {
            Enchantment ench = entry.getKey();
            if (!customMax.containsKey(ench)) {
                continue;
            }
            if (!leftIsBook && !ench.canEnchantItem(left)) {
                continue;
            }

            int max = customMax.get(ench);
            int leftLevel = getEnchantLevel(left, ench);
            int rightLevel = entry.getValue();

            int newLevel = leftLevel == rightLevel ? leftLevel + 1 : Math.max(leftLevel, rightLevel);
            newLevel = Math.min(newLevel, max);

            int currentLevel = getEnchantLevel(result, ench);
            if (newLevel <= currentLevel) {
                continue;
            }

            setEnchantLevel(result, ench, newLevel);
            modified = true;
        }

        if (modified) {
            event.setResult(result);
        }

        ItemStack finalResult = event.getResult();
        if (finalResult != null) {
            ItemStack loreApplied = finalResult.clone();
            if (applyEnchantLore(loreApplied)) {
                event.setResult(loreApplied);
            }
        }

        if (anvil.getRepairCost() >= 40) {
            anvil.setRepairCost(39);
        }
    }

    private boolean applyEnchantLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        boolean isBook = meta instanceof EnchantmentStorageMeta;
        Map<Enchantment, Integer> enchants = isBook
                ? ((EnchantmentStorageMeta) meta).getStoredEnchants()
                : item.getEnchantments();

        boolean hasAboveMax = enchants.entrySet().stream()
                .anyMatch(e -> e.getValue() > e.getKey().getMaxLevel());
        if (!hasAboveMax) {
            return false;
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<Component> lore = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            boolean isRed = level > ench.getMaxLevel();
            String key = "enchantment." + ench.getKey().getNamespace() + "." + ench.getKey().getKey();

            Component line = Component.empty()
                    .decoration(TextDecoration.ITALIC, false)
                    .color(isRed ? NamedTextColor.DARK_RED : NamedTextColor.GRAY)
                    .append(Component.translatable(key))
                    .append(level > 1 ? Component.text(" " + toRoman(level)) : Component.empty());
            lore.add(line);
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    private String toRoman(int n) {
        return n >= 0 && n < ROMAN.length ? ROMAN[n] : String.valueOf(n);
    }

    private int getEnchantLevel(ItemStack item, Enchantment ench) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof EnchantmentStorageMeta meta) {
            return meta.getStoredEnchantLevel(ench);
        }
        return item.getEnchantmentLevel(ench);
    }

    private void setEnchantLevel(ItemStack item, Enchantment ench, int level) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(ench, level, true);
            item.setItemMeta(meta);
        } else {
            item.addUnsafeEnchantment(ench, level);
        }
    }

    private Map<Enchantment, Integer> getEnchants(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof EnchantmentStorageMeta meta) {
            return meta.getStoredEnchants();
        }
        return item.getEnchantments();
    }
}
