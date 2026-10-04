package net.nitromc.api;

import org.bukkit.plugin.java.JavaPlugin;

public final class NitroApiPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("NitroApiPlugin wurde aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("NitroApiPlugin wurde deaktiviert!");
    }
}