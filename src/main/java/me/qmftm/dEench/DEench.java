package me.qmftm.dEench;

import org.bukkit.plugin.java.JavaPlugin;

public final class DEench extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
    }

    @Override
    public void onDisable() {
    }
}
