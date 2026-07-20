package me.qmftm.dEench.rules;

import me.qmftm.dEench.util.ExperienceUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Global rule: a dead player drops their full experience (not the vanilla
 * capped amount).
 */
public class XpDropListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        event.setKeepLevel(false);
        event.setNewLevel(0);
        event.setNewExp(0);
        event.setDroppedExp(ExperienceUtil.getTotalExperience(player));
    }
}
