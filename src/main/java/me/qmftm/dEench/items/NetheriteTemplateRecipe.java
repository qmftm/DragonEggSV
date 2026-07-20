package me.qmftm.dEench.items;

import me.qmftm.dEench.DEench;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers a crafting recipe for the Netherite Upgrade smithing template
 * (netherite ingot in place of the template) and disables vanilla template
 * duplication.
 *
 * <pre>
 *   D I D
 *   D N D     I = netherite ingot, N = netherrack, D = diamond
 *   D D D
 * </pre>
 */
public class NetheriteTemplateRecipe implements Listener {

    private final DEench plugin;

    public NetheriteTemplateRecipe(DEench plugin) {
        this.plugin = plugin;
        register();
    }

    private void register() {
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

    /**
     * Disable duplication: a craft that produces the template while a template
     * is present in the grid (the vanilla duplication recipe) is cancelled.
     * Our own recipe uses a netherite ingot instead, so it is unaffected.
     */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
            return;
        }
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && ingredient.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
