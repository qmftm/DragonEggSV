package me.qmftm.dEench.egg;

import java.util.UUID;

import me.qmftm.dEench.DEench;
import me.qmftm.dEench.data.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Tracks who currently holds the (single) dragon egg and who acquired it first.
 *
 * <p>Detection is reconciliation based: once per second we look for an online
 * player carrying a {@link Material#DRAGON_EGG}. If none is found the egg is
 * assumed placed/dropped/on an altar and there is no active holder.
 */
public class EggManager {

    private final DEench plugin;
    private final DataStore data;

    private UUID holder;
    private Location placedEgg;

    public EggManager(DEench plugin, DataStore data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::scan, 20L, 20L);
    }

    private void scan() {
        UUID found = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().contains(Material.DRAGON_EGG)) {
                found = player.getUniqueId();
                break;
            }
        }
        holder = found;

        if (found != null && data.getFirstHolder() == null) {
            data.setFirstHolder(found);
            data.save();
            plugin.getLogger().info("Dragon egg first acquired by " + Bukkit.getOfflinePlayer(found).getName());
        }
    }

    /** The online player currently holding the egg, or {@code null}. */
    public Player getHolder() {
        return holder == null ? null : Bukkit.getPlayer(holder);
    }

    public boolean isHolder(Player player) {
        return holder != null && holder.equals(player.getUniqueId());
    }

    public UUID getFirstHolder() {
        return data.getFirstHolder();
    }

    public boolean isFirstHolder(Player player) {
        UUID first = data.getFirstHolder();
        return first != null && first.equals(player.getUniqueId());
    }

    /** Location of the most recently known placed dragon egg block, or {@code null}. */
    public Location getPlacedEgg() {
        return placedEgg;
    }

    public void setPlacedEgg(Location location) {
        this.placedEgg = location;
    }

    public void clearPlacedEggAt(Location location) {
        if (placedEgg != null && location != null
                && placedEgg.getWorld() != null && placedEgg.getWorld().equals(location.getWorld())
                && placedEgg.getBlockX() == location.getBlockX()
                && placedEgg.getBlockY() == location.getBlockY()
                && placedEgg.getBlockZ() == location.getBlockZ()) {
            placedEgg = null;
        }
    }
}
