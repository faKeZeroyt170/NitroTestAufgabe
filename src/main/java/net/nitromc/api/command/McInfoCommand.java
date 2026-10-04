package net.nitromc.api.command;

import net.nitromc.api.model.PlayerProfile;
import net.nitromc.api.service.McApiService;
import org.bukkit.ChatColor;
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

        /*
         * Argumente überprüfen.
         */
        if (args.length != 1) {

            sender.sendMessage(
                    ChatColor.RED
                            + "Verwendung: /mcinfo <spielername>"
            );

            return true;
        }

        String playerName = args[0];

        /*
         * Lade-Nachricht.
         */
        sender.sendMessage(
                ChatColor.GRAY
                        + "Lade Informationen für "
                        + ChatColor.WHITE
                        + playerName
                        + ChatColor.GRAY
                        + "..."
        );

        /*
         * API-Anfrage starten.
         *
         * Wichtig:
         * Hier wird NICHT .join() oder .get()
         * verwendet.
         */
        CompletableFuture<PlayerProfile> future =
                apiService.getPlayerProfile(playerName);

        /*
         * Auf das Ergebnis reagieren.
         */
        future.whenComplete((profile, throwable) -> {

            /*
             * Fehler behandeln.
             */
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
                 * Bukkit/Paper-Ausgabe zurück
                 * auf den Server-Thread bringen.
                 */
                plugin.getServer()
                        .getScheduler()
                        .runTask(
                                plugin,
                                () -> sender.sendMessage(
                                        ChatColor.RED
                                                + "Fehler: "
                                                + ChatColor.GRAY
                                                + message
                                )
                        );

                return;
            }

            /*
             * Erfolgreiche Antwort.
             */
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

        sender.sendMessage("");
        sender.sendMessage(
                ChatColor.DARK_PURPLE
                        + "§lSpielerinformationen"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Name: "
                        + ChatColor.WHITE
                        + getValue(profile.name())
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "UUID: "
                        + ChatColor.WHITE
                        + getValue(profile.uuid())
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "Skin: "
                        + ChatColor.WHITE
                        + getValue(profile.skinUrl())
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "NameMC: "
                        + ChatColor.WHITE
                        + profile.getProfileUrl()
        );

        sender.sendMessage("");
    }

    private String getValue(String value) {

        if (value == null || value.isBlank()) {
            return "Nicht verfügbar";
        }

        return value;
    }
}