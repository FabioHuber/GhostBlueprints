package dev.ghostblueprints;

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Chat helpers using the Spigot-compatible legacy color codes. Messages may contain simple
 * tags such as {@code <aqua>...</aqua>}; a closing tag returns to the message's base color.
 */
public final class Messages {
    private static final String PREFIX = "<dark_gray>[<aqua>Ghost</aqua>]</dark_gray> ";
    private static final Map<String, ChatColor> TAGS = Map.of(
            "white", ChatColor.WHITE,
            "gray", ChatColor.GRAY,
            "dark_gray", ChatColor.DARK_GRAY,
            "aqua", ChatColor.AQUA,
            "green", ChatColor.GREEN,
            "red", ChatColor.RED);

    private Messages() {
    }

    public static void info(CommandSender to, String message) {
        to.sendMessage(colorize(PREFIX + "<gray>" + message, ChatColor.GRAY));
    }

    public static void error(CommandSender to, String message) {
        to.sendMessage(colorize(PREFIX + "<red>" + message, ChatColor.RED));
    }

    public static void raw(CommandSender to, String message) {
        to.sendMessage(colorize(message, ChatColor.WHITE));
    }

    public static String colorize(String message, ChatColor base) {
        String result = message;
        for (Map.Entry<String, ChatColor> tag : TAGS.entrySet()) {
            result = result.replace("</" + tag.getKey() + ">", base.toString());
            result = result.replace("<" + tag.getKey() + ">", tag.getValue().toString());
        }
        return result;
    }

    /** Neutralizes color tags in user-controlled text so it cannot recolor the message. */
    public static String escape(String text) {
        return text.replace('<', '‹').replace('>', '›');
    }
}
