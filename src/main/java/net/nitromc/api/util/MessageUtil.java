package net.nitromc.api.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final String PREFIX =
            ChatColor.DARK_PURPLE
                    + "NitroMC "
                    + ChatColor.DARK_GRAY
                    + "» "
                    + ChatColor.GRAY;

    private MessageUtil() {
        // Utility-Klasse
    }

    public static void send(
            CommandSender sender,
            String message
    ) {
        sender.sendMessage(
                PREFIX + colorize(message)
        );
    }

    public static void sendError(
            CommandSender sender,
            String message
    ) {
        sender.sendMessage(
                PREFIX
                        + ChatColor.RED
                        + message
        );
    }

    public static void sendSuccess(
            CommandSender sender,
            String message
    ) {
        sender.sendMessage(
                PREFIX
                        + ChatColor.GREEN
                        + message
        );
    }

    public static void sendInfo(
            CommandSender sender,
            String message
    ) {
        sender.sendMessage(
                PREFIX
                        + ChatColor.GRAY
                        + message
        );
    }

    private static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                message
        );
    }
}