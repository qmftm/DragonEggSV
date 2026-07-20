package me.qmftm.dEench.egg;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import me.qmftm.dEench.DEench;
import me.qmftm.dEench.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * When the egg holder dies, the dragon egg is enshrined on an altar at the
 * death location with a purple beacon beam that lasts 100 real minutes, so
 * other players can find the fallen egg.
 */
public class DeathAltarService implements Listener {

    private final DEench plugin;
    private final EggManager eggManager;
    private final PluginConfig config;

    public DeathAltarService(DEench plugin, EggManager eggManager, PluginConfig config) {
        this.plugin = plugin;
        this.eggManager = eggManager;
        this.config = config;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!eggManager.isHolder(player)) {
            return;
        }

        boolean hadEgg = false;
        for (Iterator<ItemStack> it = event.getDrops().iterator(); it.hasNext(); ) {
            if (it.next().getType() == Material.DRAGON_EGG) {
                it.remove();
                hadEgg = true;
            }
        }
        if (!hadEgg) {
            return;
        }

        buildAltar(player.getLocation());
    }

    private void buildAltar(Location death) {
        World world = death.getWorld();
        if (world == null) {
            return;
        }

        int cx = death.getBlockX();
        int cz = death.getBlockZ();
        int cy = death.getBlockY();
        while (cy > world.getMinHeight() + 1 && world.getBlockAt(cx, cy - 1, cz).isPassable()) {
            cy--;
        }

        Map<Location, BlockData> snapshot = new LinkedHashMap<>();

        // Base layer: 3x3 iron (beacon power tier 1).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(world, cx + dx, cy, cz + dz, Material.IRON_BLOCK, snapshot);
            }
        }
        // Platform ring: quartz around the beacon.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                set(world, cx + dx, cy + 1, cz + dz, Material.QUARTZ_BLOCK, snapshot);
            }
        }
        // Beacon + purple glass to tint the beam.
        set(world, cx, cy + 1, cz, Material.BEACON, snapshot);
        set(world, cx, cy + 2, cz, Material.PURPLE_STAINED_GLASS, snapshot);

        // The egg, enshrined on a corner pedestal beside the beam.
        Location eggLoc = new Location(world, cx + 1, cy + 2, cz + 1);
        set(world, eggLoc.getBlockX(), eggLoc.getBlockY(), eggLoc.getBlockZ(), Material.DRAGON_EGG, snapshot);
        eggManager.setPlacedEgg(eggLoc);

        long ticks = Math.max(1L, (long) config.altarBeaconMinutes()) * 60L * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> teardown(world, eggLoc, snapshot), ticks);
    }

    private void teardown(World world, Location eggLoc, Map<Location, BlockData> snapshot) {
        boolean eggStillHere = eggLoc.getBlock().getType() == Material.DRAGON_EGG;

        for (Map.Entry<Location, BlockData> entry : snapshot.entrySet()) {
            Location loc = entry.getKey();
            boolean isEgg = loc.getBlockX() == eggLoc.getBlockX()
                    && loc.getBlockY() == eggLoc.getBlockY()
                    && loc.getBlockZ() == eggLoc.getBlockZ();
            if (isEgg) {
                continue;
            }
            loc.getBlock().setBlockData(entry.getValue(), false);
        }

        if (eggStillHere) {
            eggLoc.getBlock().setType(Material.AIR, false);
            world.dropItem(eggLoc.clone().add(0.5, 0.2, 0.5), new ItemStack(Material.DRAGON_EGG));
        }
        eggManager.clearPlacedEggAt(eggLoc);
    }

    private void set(World world, int x, int y, int z, Material material, Map<Location, BlockData> snapshot) {
        Block block = world.getBlockAt(x, y, z);
        snapshot.put(block.getLocation(), block.getBlockData().clone());
        block.setType(material, false);
    }
}
