package net.nitromc.api.command;

import net.nitromc.api.model.NameHistoryEntry;
import net.nitromc.api.model.PlayerProfile;
import net.nitromc.api.service.McApiService;
import net.nitromc.api.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NameHistoryCommand implements CommandExecutor {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    private final McApiService apiService;
    private final Plugin plugin;

    public NameHistoryCommand(
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
                    "Verwendung: /namehistory <spielername>"
            );

            return true;
        }

        String playerName = args[0];

        MessageUtil.send(
                sender,
                "&7Lade Namenshistorie für &f"
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
                            () -> sendNameHistory(
                                    sender,
                                    profile
                            )
                    );
        });

        return true;
    }

    private void sendNameHistory(
            CommandSender sender,
            PlayerProfile profile
    ) {

        if (profile.names().isEmpty()) {

            MessageUtil.send(
                    sender,
                    "&7Keine Namenshistorie gefunden."
            );

            return;
        }

        List<NameHistoryEntry> entries =
                profile.names()
                        .entrySet()
                        .stream()
                        .map(entry ->
                                new NameHistoryEntry(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                        )
                        .sorted(
                                Comparator.comparingLong(
                                        NameHistoryEntry::changedAt
                                ).reversed()
                        )
                        .toList();

        MessageUtil.send(
                sender,
                "&5&lNamenshistorie"
        );

        MessageUtil.send(
                sender,
                "&7Spieler: &f"
                        + getValue(profile.name())
        );

        MessageUtil.send(
                sender,
                "&7Einträge: &f"
                        + entries.size()
        );

        sender.sendMessage("");

        for (NameHistoryEntry entry : entries) {

            String date = formatDate(
                    entry.changedAt()
            );

            MessageUtil.send(
                    sender,
                    "&7• &f"
                            + entry.name()
                            + " &8- &7"
                            + date
            );
        }
    }

    private String formatDate(long timestamp) {

        if (timestamp <= 0) {
            return "Unbekannt";
        }

        try {
            return DATE_FORMAT.format(
                    Instant.ofEpochMilli(timestamp)
            );
        } catch (Exception exception) {
            return "Unbekannt";
        }
    }

    private String getValue(String value) {

        if (value == null || value.isBlank()) {
            return "Nicht verfügbar";
        }

        return value;
    }
}