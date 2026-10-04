package net.nitromc.api;

import net.nitromc.api.command.McInfoCommand;
import net.nitromc.api.service.McApiService;
import org.bukkit.plugin.java.JavaPlugin;

public final class NitroApiPlugin extends JavaPlugin {

    private McApiService apiService;

    @Override
    public void onEnable() {

        /*
         * API-Service erstellen.
         */
        apiService = new McApiService();

        /*
         * /mcinfo registrieren.
         */
        getCommand("mcinfo").setExecutor(
                new McInfoCommand(
                        apiService,
                        this
                )
        );

        getLogger().info(
                "NitroApiPlugin wurde aktiviert!"
        );
    }

    @Override
    public void onDisable() {

        getLogger().info(
                "NitroApiPlugin wurde deaktiviert!"
        );
    }
}