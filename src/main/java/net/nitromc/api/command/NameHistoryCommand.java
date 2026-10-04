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

        apiService.getPlayerProfile(playerName)
                .thenCompose(profile ->
                        apiService.getNameHistory(
                                profile.uuid()
                        )
                )
                .whenComplete((entries, throwable) -> {

                    plugin.getServer()
                            .getScheduler()
                            .runTask(
                                    plugin,
                                    () -> {

                                        if (throwable != null) {

                                            Throwable cause = throwable;

                                            if (throwable.getCause() != null) {
                                                cause = throwable.getCause();
                                            }

                                            String message =
                                                    cause.getMessage() != null
                                                            ? cause.getMessage()
                                                            : "Unbekannter API-Fehler.";

                                            MessageUtil.sendError(
                                                    sender,
                                                    "Fehler: " + message
                                            );

                                            return;
                                        }

                                        sendNameHistory(
                                                sender,
                                                entries
                                        );
                                    }
                            );
                });

        return true;
    }

    private void sendNameHistory(
            CommandSender sender,
            List<NameHistoryEntry> entries
    ) {

        if (entries.isEmpty()) {

            MessageUtil.send(
                    sender,
                    "&7Keine Namenshistorie gefunden."
            );

            return;
        }

        entries = entries.stream()
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

        for (NameHistoryEntry entry : entries) {

            MessageUtil.send(
                    sender,
                    "&7• &f"
                            + entry.name()
                            + " &8- &7"
                            + formatDate(
                            entry.changedAt()
                    )
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
}