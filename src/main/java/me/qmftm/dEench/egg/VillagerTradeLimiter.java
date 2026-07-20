package me.qmftm.dEench.egg;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import me.qmftm.dEench.config.PluginConfig;
import me.qmftm.dEench.util.Worlds;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Limits the egg holder to a fixed number of villager trades per MC day.
 */
public class VillagerTradeLimiter implements Listener {

    private final EggManager eggManager;
    private final PluginConfig config;
    private final Map<UUID, Counter> counters = new HashMap<>();

    public VillagerTradeLimiter(EggManager eggManager, PluginConfig config) {
        this.eggManager = eggManager;
        this.config = config;
    }

    @EventHandler
    public void onPurchase(PlayerPurchaseEvent event) {
        Player player = event.getPlayer();
        if (!eggManager.isHolder(player)) {
            return;
        }

        long day = currentDay();
        Counter counter = counters.computeIfAbsent(player.getUniqueId(), k -> new Counter());
        if (counter.day != day) {
            counter.day = day;
            counter.count = 0;
        }

        if (counter.count >= config.villagerTradesPerDay()) {
            event.setCancelled(true);
            return;
        }
        counter.count++;
    }

    private long currentDay() {
        World overworld = Worlds.overworld();
        return overworld == null ? 0L : overworld.getFullTime() / 24000L;
    }

    private static final class Counter {
        private long day = -1L;
        private int count;
    }
}
