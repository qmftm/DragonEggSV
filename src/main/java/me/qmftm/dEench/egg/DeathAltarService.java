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
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * When the egg holder dies, the dragon egg is enshrined on an altar at the
 * death location with a purple beacon beam that lasts 100 real minutes, so
 * other players can find the fallen egg.
 *
 * <p>Layout: a 3x3 iron base powers a central beacon ringed by cobblestone /
 * mossy-cobblestone stairs (high side toward the beacon). Purple glass tints
 * the beam and a barrier column runs up to y=319 to keep the beam's path clear
 * and un-grief-able. The egg sits on a pedestal on one corner.
 */
public class DeathAltarService implements Listener {

    private static final int BEAM_TOP_Y = 319;

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

        // Base: 3x3 iron (beacon power tier 1).
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setBlock(world, cx + dx, cy, cz + dz, Material.IRON_BLOCK, snapshot);
            }
        }

        // Ring of stairs around the beacon, high side facing the beacon.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Material mat = (dx != 0 && dz != 0)
                        ? Material.MOSSY_COBBLESTONE_STAIRS
                        : Material.COBBLESTONE_STAIRS;
                setStair(world, cx + dx, cy + 1, cz + dz, mat, faceTowardCenter(dx, dz), snapshot);
            }
        }

        // Beacon + purple glass (beam tint).
        setBlock(world, cx, cy + 1, cz, Material.BEACON, snapshot);
        setBlock(world, cx, cy + 2, cz, Material.PURPLE_STAINED_GLASS, snapshot);

        // Barrier column keeping the beam path clear up to build height.
        int top = Math.min(BEAM_TOP_Y, world.getMaxHeight() - 1);
        for (int y = cy + 3; y <= top; y++) {
            setBlock(world, cx, y, cz, Material.BARRIER, snapshot);
        }

        // Egg pedestal on a corner, clear of the beam.
        setBlock(world, cx + 1, cy + 2, cz + 1, Material.COBBLESTONE, snapshot);
        Location eggLoc = new Location(world, cx + 1, cy + 3, cz + 1);
        setBlock(world, eggLoc.getBlockX(), eggLoc.getBlockY(), eggLoc.getBlockZ(), Material.DRAGON_EGG, snapshot);
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

    private BlockFace faceTowardCenter(int dx, int dz) {
        if (dx != 0 && dz != 0) {
            return dz > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
        }
        if (dx > 0) {
            return BlockFace.WEST;
        }
        if (dx < 0) {
            return BlockFace.EAST;
        }
        return dz > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    private void setStair(World world, int x, int y, int z, Material material, BlockFace face,
                          Map<Location, BlockData> snapshot) {
        Block block = world.getBlockAt(x, y, z);
        snapshot.put(block.getLocation(), block.getBlockData().clone());
        BlockData data = material.createBlockData();
        if (data instanceof Stairs stairs) {
            stairs.setFacing(face);
        }
        block.setBlockData(data, false);
    }

    private void setBlock(World world, int x, int y, int z, Material material, Map<Location, BlockData> snapshot) {
        Block block = world.getBlockAt(x, y, z);
        snapshot.put(block.getLocation(), block.getBlockData().clone());
        block.setType(material, false);
    }
}
