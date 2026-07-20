package me.qmftm.dEench.items;

import java.util.Map;

import me.qmftm.dEench.DEench;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Colors an equipment item's name red while it carries any enchantment at its
 * effective maximum level (the custom overench max if defined, else vanilla).
 */
public class MaxEnchantNameService {

    private final DEench plugin;
    private final Map<Enchantment, Integer> overenchMax;
    private final NamespacedKey markerKey;

    public MaxEnchantNameService(DEench plugin, Map<Enchantment, Integer> overenchMax) {
        this.plugin = plugin;
        this.overenchMax = overenchMax;
        this.markerKey = new NamespacedKey(plugin, "max_ench_red");
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::scan, 40L, 40L);
    }

    private void scan() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                process(item);
            }
        }
    }

    private void process(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta instanceof EnchantmentStorageMeta) {
            return; // skip enchanted books — those aren't equipment
        }

        boolean hasMax = false;
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            if (entry.getValue() >= effectiveMax(entry.getKey())) {
                hasMax = true;
                break;
            }
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean marked = pdc.has(markerKey, PersistentDataType.BYTE);

        if (hasMax && !marked) {
            Component base = meta.hasDisplayName()
                    ? meta.displayName()
                    : Component.translatable(item.getType().translationKey());
            meta.displayName(base.color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        } else if (!hasMax && marked) {
            meta.displayName(null);
            pdc.remove(markerKey);
            item.setItemMeta(meta);
        }
    }

    private int effectiveMax(Enchantment ench) {
        Integer custom = overenchMax.get(ench);
        return custom != null ? custom : ench.getMaxLevel();
    }
}
