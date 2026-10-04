package net.nitromc.api.command;

import net.nitromc.api.model.PlayerProfile;
import net.nitromc.api.service.McApiService;
import net.nitromc.api.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public class McInfoCommand implements CommandExecutor {

    private final McApiService apiService;
    private final Plugin plugin;

    public McInfoCommand(
            McApiService apiService,
            Plugin plugin
    ) {
        this.apiService = apiService;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length != 1) {

            MessageUtil.sendError(
                    sender,
                    "Verwendung: /mcinfo <spielername>"
            );

            return true;
        }

        String playerName = args[0];

        MessageUtil.send(
                sender,
                "&7Lade Informationen für &f"
                        + playerName
                        + "&7..."
        );

        CompletableFuture<PlayerProfile> future =
                apiService.getPlayerProfile(playerName);

        future.whenComplete((profile, throwable) -> {

            if (throwable != null) {

                Throwable cause = throwable;

                if (throwable instanceof
                        java.util.concurrent.CompletionException) {

                    cause = throwable.getCause();
                }

                String message =
                        cause != null
                                && cause.getMessage() != null
                                ? cause.getMessage()
                                : "Unbekannter API-Fehler.";

                /*
                 * Bukkit-Nachrichten müssen auf dem
                 * Main-Thread ausgeführt werden.
                 */
                plugin.getServer()
                        .getScheduler()
                        .runTask(
                                plugin,
                                () -> MessageUtil.sendError(
                                        sender,
                                        "Fehler: " + message
                                )
                        );

                return;
            }

            plugin.getServer()
                    .getScheduler()
                    .runTask(
                            plugin,
                            () -> sendPlayerInfo(
                                    sender,
                                    profile
                            )
                    );
        });

        return true;
    }

    private void sendPlayerInfo(
            CommandSender sender,
            PlayerProfile profile
    ) {

        MessageUtil.send(
                sender,
                "&5&lSpielerinformationen"
        );

        MessageUtil.send(
                sender,
                "&7Name: &f"
                        + getValue(profile.name())
        );

        MessageUtil.send(
                sender,
                "&7UUID: &f"
                        + getValue(profile.uuid())
        );

        MessageUtil.send(
                sender,
                "&7Skin: &f"
                        + getValue(profile.skinUrl())
        );

        MessageUtil.send(
                sender,
                "&7NameMC: &f"
                        + profile.getProfileUrl()
        );
    }

    private String getValue(String value) {

        if (value == null || value.isBlank()) {
            return "Nicht verfügbar";
        }

        return value;
    }
}