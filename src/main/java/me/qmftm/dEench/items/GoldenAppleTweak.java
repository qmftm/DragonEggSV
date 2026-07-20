package me.qmftm.dEench.items;

import me.qmftm.dEench.DEench;
import me.qmftm.dEench.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Shortens the Regeneration granted by eating a golden apple (default 2s).
 */
public class GoldenAppleTweak implements Listener {

    private final DEench plugin;
    private final PluginConfig config;

    public GoldenAppleTweak(DEench plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.GOLDEN_APPLE) {
            return;
        }
        Player player = event.getPlayer();
        int durationTicks = Math.max(0, config.goldenAppleRegenSeconds()) * 20;

        // Re-apply on the next tick, after vanilla has granted its effect.
        Bukkit.getScheduler().runTask(plugin, () -> {
            PotionEffect regen = player.getPotionEffect(PotionEffectType.REGENERATION);
            if (regen == null) {
                return;
            }
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, durationTicks, regen.getAmplifier(), true, true, true));
        });
    }
}
