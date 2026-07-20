package me.qmftm.dEench.items;

import me.qmftm.dEench.DEench;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers a crafting recipe for the Netherite Upgrade smithing template,
 * mirroring the vanilla duplication recipe but using a netherite ingot in
 * place of an existing template.
 *
 * <pre>
 *   D I D
 *   D N D     I = netherite ingot, N = netherrack, D = diamond
 *   D D D
 * </pre>
 */
public final class NetheriteTemplateRecipe {

    private NetheriteTemplateRecipe() {
    }

    public static void register(DEench plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "netherite_upgrade_template");
        if (plugin.getServer().getRecipe(key) != null) {
            return;
        }

        ItemStack result = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("DID", "DND", "DDD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('I', Material.NETHERITE_INGOT);
        recipe.setIngredient('N', Material.NETHERRACK);
        plugin.getServer().addRecipe(recipe);
    }
}
