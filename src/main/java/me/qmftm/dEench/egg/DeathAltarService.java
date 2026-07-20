package me.qmftm.dEench.egg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.qmftm.dEench.DEench;
import me.qmftm.dEench.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * When the egg holder dies, the dragon egg is enshrined on an altar at the
 * death location with a purple beacon beam, so other players can find the
 * fallen egg. A floating text display counts down to the altar's removal.
 *
 * <p>Altars are persisted to altars.yml (including the block snapshot needed to
 * restore terrain), so the countdown and teardown survive server restarts.
 */
public class DeathAltarService implements Listener {

    private static final int BEAM_TOP_Y = 319;
    private static final String TIMER_TAG = "de_altar_timer";

    private final DEench plugin;
    private final EggManager eggManager;
    private final PluginConfig config;
    private final File file;
    private final List<Altar> altars = new ArrayList<>();

    public DeathAltarService(DEench plugin, EggManager eggManager, PluginConfig config) {
        this.plugin = plugin;
        this.eggManager = eggManager;
        this.config = config;
        this.file = new File(plugin.getDataFolder(), "altars.yml");
    }

    public void start() {
        removeStaleTimers();
        load();
        for (Altar altar : altars) {
            eggManager.setPlacedEgg(altar.eggLocation());
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
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

        long expireAt = System.currentTimeMillis() + Math.max(1L, (long) config.altarBeaconMinutes()) * 60_000L;
        Altar altar = new Altar(UUID.randomUUID().toString(), world.getName(),
                eggLoc.getBlockX(), eggLoc.getBlockY(), eggLoc.getBlockZ(), expireAt, snapshot);
        altar.display = spawnTimerDisplay(altar);
        altars.add(altar);
        save();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean dirty = false;
        for (Iterator<Altar> it = altars.iterator(); it.hasNext(); ) {
            Altar altar = it.next();
            long remaining = altar.expireAt - now;
            if (remaining <= 0) {
                teardown(altar);
                it.remove();
                dirty = true;
                continue;
            }
            if (altar.display == null || !altar.display.isValid()) {
                altar.display = spawnTimerDisplay(altar);
            }
            if (altar.display != null) {
                altar.display.text(timerText(remaining));
            }
        }
        if (dirty) {
            save();
        }
    }

    private void teardown(Altar altar) {
        World world = Bukkit.getWorld(altar.world);
        Location eggLoc = altar.eggLocation();
        boolean eggStillHere = eggLoc != null && eggLoc.getBlock().getType() == Material.DRAGON_EGG;

        if (world != null) {
            for (Map.Entry<Location, BlockData> entry : altar.snapshot.entrySet()) {
                Location loc = entry.getKey();
                if (sameBlock(loc, altar.eggX, altar.eggY, altar.eggZ)) {
                    continue;
                }
                loc.getBlock().setBlockData(entry.getValue(), false);
            }
            if (eggStillHere) {
                eggLoc.getBlock().setType(Material.AIR, false);
                world.dropItem(eggLoc.clone().add(0.5, 0.2, 0.5), new ItemStack(Material.DRAGON_EGG));
            }
        }

        if (altar.display != null && altar.display.isValid()) {
            altar.display.remove();
        }
        if (eggLoc != null) {
            eggManager.clearPlacedEggAt(eggLoc);
        }
    }

    private TextDisplay spawnTimerDisplay(Altar altar) {
        World world = Bukkit.getWorld(altar.world);
        if (world == null) {
            return null;
        }
        Location loc = new Location(world, altar.eggX + 0.5, altar.eggY + 1.4, altar.eggZ + 0.5);
        return world.spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(true);
            td.setViewRange(1.0f);
            td.setPersistent(false);
            td.addScoreboardTag(TIMER_TAG);
            td.text(timerText(altar.expireAt - System.currentTimeMillis()));
        });
    }

    private Component timerText(long remainingMs) {
        long totalSeconds = Math.max(0L, remainingMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return Component.text("🥚 드래곤 알 제단", NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("⏳ " + minutes + "분 " + seconds + "초 후 소멸", NamedTextColor.LIGHT_PURPLE));
    }

    private void removeStaleTimers() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof TextDisplay && entity.getScoreboardTags().contains(TIMER_TAG)) {
                    entity.remove();
                }
            }
        }
    }

    // --- persistence ---------------------------------------------------------

    private void load() {
        if (!file.exists()) {
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("altars");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }
            Map<Location, BlockData> snapshot = new LinkedHashMap<>();
            World world = Bukkit.getWorld(sec.getString("world", ""));
            if (world != null) {
                for (String raw : sec.getStringList("blocks")) {
                    String[] parts = raw.split(";", 4);
                    if (parts.length < 4) {
                        continue;
                    }
                    try {
                        Location loc = new Location(world, Integer.parseInt(parts[0]),
                                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        snapshot.put(loc, Bukkit.createBlockData(parts[3]));
                    } catch (IllegalArgumentException ignored) {
                        // skip malformed entry
                    }
                }
            }
            altars.add(new Altar(id, sec.getString("world", ""),
                    sec.getInt("egg.x"), sec.getInt("egg.y"), sec.getInt("egg.z"),
                    sec.getLong("expire"), snapshot));
        }
    }

    private void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Altar altar : altars) {
            String base = "altars." + altar.id + ".";
            cfg.set(base + "world", altar.world);
            cfg.set(base + "egg.x", altar.eggX);
            cfg.set(base + "egg.y", altar.eggY);
            cfg.set(base + "egg.z", altar.eggZ);
            cfg.set(base + "expire", altar.expireAt);
            List<String> blocks = new ArrayList<>();
            for (Map.Entry<Location, BlockData> entry : altar.snapshot.entrySet()) {
                Location loc = entry.getKey();
                blocks.add(loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ()
                        + ";" + entry.getValue().getAsString());
            }
            cfg.set(base + "blocks", blocks);
        }
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save altars.yml: " + e.getMessage());
        }
    }

    // --- helpers -------------------------------------------------------------

    private static boolean sameBlock(Location loc, int x, int y, int z) {
        return loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z;
    }

    private BlockFace faceTowardCenter(int dx, int dz) {
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

    private static final class Altar {
        private final String id;
        private final String world;
        private final int eggX;
        private final int eggY;
        private final int eggZ;
        private final long expireAt;
        private final Map<Location, BlockData> snapshot;
        private TextDisplay display;

        private Altar(String id, String world, int eggX, int eggY, int eggZ, long expireAt,
                      Map<Location, BlockData> snapshot) {
            this.id = id;
            this.world = world;
            this.eggX = eggX;
            this.eggY = eggY;
            this.eggZ = eggZ;
            this.expireAt = expireAt;
            this.snapshot = snapshot;
        }

        private Location eggLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, eggX, eggY, eggZ);
        }
    }
}
