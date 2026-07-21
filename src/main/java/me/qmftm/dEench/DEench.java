package me.qmftm.dEench;

import java.util.HashMap;
import java.util.Map;

import me.qmftm.dEench.config.PluginConfig;
import me.qmftm.dEench.data.DataStore;
import me.qmftm.dEench.egg.BeaconAmbienceService;
import me.qmftm.dEench.egg.DeathAltarService;
import me.qmftm.dEench.egg.DimensionLockListener;
import me.qmftm.dEench.egg.EggBlockListener;
import me.qmftm.dEench.egg.EggContainmentListener;
import me.qmftm.dEench.egg.EggManager;
import me.qmftm.dEench.egg.EnderEffectService;
import me.qmftm.dEench.egg.FootprintService;
import me.qmftm.dEench.egg.VillagerTradeLimiter;
import me.qmftm.dEench.game.GameClock;
import me.qmftm.dEench.game.WorldBorderService;
import me.qmftm.dEench.game.WorldRulesService;
import me.qmftm.dEench.items.BannedEnchantListener;
import me.qmftm.dEench.items.BannedItemListener;
import me.qmftm.dEench.items.DragonRespawnXp;
import me.qmftm.dEench.items.GoldenAppleTweak;
import me.qmftm.dEench.items.MaxEnchantNameService;
import me.qmftm.dEench.items.NetheriteTemplateRecipe;
import me.qmftm.dEench.items.PotionCarryLimiter;
import me.qmftm.dEench.rules.EndPortalListener;
import me.qmftm.dEench.rules.ExplosionDamageListener;
import me.qmftm.dEench.rules.InfoLeakListener;
import me.qmftm.dEench.rules.XpDropListener;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DEench extends JavaPlugin {

    private final Map<Enchantment, Integer> overenchMax = new HashMap<>();

    private DataStore dataStore;
    private EggManager eggManager;
    private FootprintService footprintService;
    private GameClock gameClock;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadOverench();

        dataStore = new DataStore(this);
        dataStore.load();
        PluginConfig config = new PluginConfig(this);

        eggManager = new EggManager(this, dataStore);
        footprintService = new FootprintService(this, eggManager, config);
        gameClock = new GameClock(this, dataStore, config, eggManager);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new AnvilListener(this), this);
        pm.registerEvents(new DimensionLockListener(eggManager), this);
        pm.registerEvents(new EggBlockListener(eggManager), this);
        pm.registerEvents(new EggContainmentListener(), this);
        DeathAltarService deathAltarService = new DeathAltarService(this, eggManager, config);
        pm.registerEvents(deathAltarService, this);
        pm.registerEvents(new VillagerTradeLimiter(eggManager, config), this);
        pm.registerEvents(new XpDropListener(), this);
        pm.registerEvents(new InfoLeakListener(), this);
        pm.registerEvents(new ExplosionDamageListener(config), this);
        pm.registerEvents(new GoldenAppleTweak(this, config), this);
        pm.registerEvents(new PotionCarryLimiter(config), this);
        pm.registerEvents(new EndPortalListener(), this);
        BannedItemListener bannedItems = new BannedItemListener(this);
        pm.registerEvents(bannedItems, this);
        BannedEnchantListener bannedEnchants = new BannedEnchantListener(this, config.bannedEnchants());
        pm.registerEvents(bannedEnchants, this);
        pm.registerEvents(new DragonRespawnXp(config), this);
        pm.registerEvents(new NetheriteTemplateRecipe(this), this);

        WorldRulesService worldRules = new WorldRulesService();
        pm.registerEvents(worldRules, this);
        worldRules.applyAll();
        new WorldBorderService(config).applyAll();

        bannedItems.start();
        bannedEnchants.start();

        eggManager.start();
        deathAltarService.start();
        new EnderEffectService(this, eggManager).start();
        new BeaconAmbienceService(this, eggManager).start();
        new MaxEnchantNameService(this, overenchMax).start();
        footprintService.start();
        gameClock.start();

        PluginCommand command = getCommand("DE");
        if (command != null) {
            DECommand executor = new DECommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    public GameClock getGameClock() {
        return gameClock;
    }

    public EggManager getEggManager() {
        return eggManager;
    }

    @Override
    public void onDisable() {
        if (footprintService != null) {
            footprintService.shutdown();
        }
        if (gameClock != null) {
            gameClock.shutdown();
        }
        if (dataStore != null) {
            dataStore.save();
        }
    }

    /**
     * Custom maximum enchantment levels used by the anvil combine logic.
     */
    public Map<Enchantment, Integer> getOverenchMax() {
        return overenchMax;
    }

    /**
     * (Re)loads the {@code overench} section of config.yml into memory.
     */
    public void loadOverench() {
        overenchMax.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("overench");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Enchantment ench = resolveEnchantment(key);
            if (ench == null) {
                getLogger().warning("Unknown enchantment in config (overench." + key + "), skipping.");
                continue;
            }
            overenchMax.put(ench, section.getInt(key));
        }
    }

    /**
     * Updates a custom max level both in memory and in config.yml.
     */
    public void setOverench(Enchantment ench, int level) {
        overenchMax.put(ench, level);
        getConfig().set("overench." + configKey(ench), level);
        saveConfig();
    }

    private static String configKey(Enchantment ench) {
        NamespacedKey key = ench.getKey();
        return key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.asString();
    }

    /**
     * Resolves an enchantment from a name such as {@code sharpness} or
     * {@code minecraft:sharpness}. Returns {@code null} if it does not exist.
     */
    public static Enchantment resolveEnchantment(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.toLowerCase();
        NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        if (key == null) {
            return null;
        }
        return Registry.ENCHANTMENT.get(key);
    }
}
