package me.qmftm.dEench.rules;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * Global rule: block information that could reveal player positions or
 * activity — chat, death messages, and advancement announcements. (Coordinate
 * display is handled by the reducedDebugInfo gamerule in WorldRulesService.)
 */
public class InfoLeakListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        event.message(null);
    }
}
