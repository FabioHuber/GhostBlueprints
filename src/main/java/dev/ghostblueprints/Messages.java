package dev.ghostblueprints;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/** MiniMessage helpers. User-controlled values are escaped so they cannot inject formatting. */
public final class Messages {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<dark_gray>[<aqua>Ghost</aqua>]</dark_gray> ";

    private Messages() {
    }

    public static void info(CommandSender to, String message) {
        to.sendMessage(MINI.deserialize(PREFIX + "<gray>" + message));
    }

    public static void error(CommandSender to, String message) {
        to.sendMessage(MINI.deserialize(PREFIX + "<red>" + message));
    }

    public static void raw(CommandSender to, String message) {
        to.sendMessage(MINI.deserialize(message));
    }

    public static Component component(String message) {
        return MINI.deserialize(message);
    }

    public static String escape(String text) {
        return MINI.escapeTags(text);
    }
}
