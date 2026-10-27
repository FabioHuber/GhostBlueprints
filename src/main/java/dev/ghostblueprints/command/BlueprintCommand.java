package dev.ghostblueprints.command;

import dev.ghostblueprints.BlueprintLibrary;
import dev.ghostblueprints.GhostBlueprintsPlugin;
import dev.ghostblueprints.Messages;
import dev.ghostblueprints.render.BlockMatcher;
import dev.ghostblueprints.render.GhostRenderer;
import dev.ghostblueprints.render.ProgressScanner;
import dev.ghostblueprints.schematic.Blueprint;
import dev.ghostblueprints.schematic.SchematicException;
import dev.ghostblueprints.schematic.SchematicLoader;
import dev.ghostblueprints.session.BlueprintSession;
import dev.ghostblueprints.session.SessionManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Handles /blueprint and its subcommands. */
public final class BlueprintCommand implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of(
            "list", "load", "place", "move", "rotate", "layer", "hide", "show", "status", "materials", "clear",
            "reload", "help");
    private static final int MATERIAL_LINES = 15;

    private final GhostBlueprintsPlugin plugin;
    private final SessionManager sessions;
    private final GhostRenderer renderer;
    private final BlueprintLibrary library;

    public BlueprintCommand(
            GhostBlueprintsPlugin plugin, SessionManager sessions, GhostRenderer renderer, BlueprintLibrary library) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.renderer = renderer;
        this.library = library;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "This command can only be used by players.");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> list(player);
            case "load" -> load(player, args);
            case "place" -> place(player);
            case "move" -> move(player, args);
            case "rotate" -> rotate(player, args);
            case "layer" -> layer(player, args);
            case "hide" -> setVisible(player, false);
            case "show" -> setVisible(player, true);
            case "status" -> status(player);
            case "materials" -> materials(player);
            case "clear" -> clear(player);
            case "reload" -> reload(player);
            default -> help(player);
        }
        return true;
    }

    private void help(Player player) {
        Messages.raw(player, "<dark_gray>--- <aqua>Ghost Blueprints</aqua> ---");
        Messages.raw(player, "<aqua>/bp list</aqua> <gray>- available schematics");
        Messages.raw(player, "<aqua>/bp load <name></aqua> <gray>- load a schematic");
        Messages.raw(player, "<aqua>/bp place</aqua> <gray>- anchor it at the block you look at");
        Messages.raw(player, "<aqua>/bp move <dx> <dy> <dz></aqua> <gray>- shift by blocks");
        Messages.raw(player, "<aqua>/bp rotate [90|180|270]</aqua> <gray>- rotate clockwise");
        Messages.raw(player, "<aqua>/bp layer <n|all|up|down></aqua> <gray>- show a single layer");
        Messages.raw(player, "<aqua>/bp status</aqua> <gray>- progress");
        Messages.raw(player, "<aqua>/bp materials</aqua> <gray>- what you still need");
        Messages.raw(player, "<aqua>/bp hide | show | clear</aqua>");
    }

    private void list(Player player) {
        try {
            List<String> names = library.names();
            if (names.isEmpty()) {
                Messages.info(player, "No schematics found. Put .schem files into plugins/GhostBlueprints/blueprints.");
                return;
            }
            Messages.info(player, "Schematics: <white>" + Messages.escape(String.join(", ", names)));
        } catch (IOException e) {
            Messages.error(player, "Could not list schematics: " + Messages.escape(String.valueOf(e.getMessage())));
        }
    }

    private void load(Player player, String[] args) {
        if (args.length < 2) {
            Messages.error(player, "Usage: /bp load <name>");
            return;
        }
        String name = args[1];
        Optional<Path> file = library.find(name);
        if (file.isEmpty()) {
            Messages.error(player, "No schematic named <white>" + Messages.escape(name) + "</white>. Try /bp list.");
            return;
        }
        Messages.info(player, "Loading <white>" + Messages.escape(name) + "</white>...");
        int maxVolume = plugin.settings().maxVolume();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Blueprint blueprint = SchematicLoader.load(name, file.get(), maxVolume);
                Bukkit.getScheduler().runTask(plugin, () -> onLoaded(player, blueprint));
            } catch (SchematicException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        Messages.error(player, "Failed to load: " + Messages.escape(e.getMessage())));
            }
        });
    }

    private void onLoaded(Player player, Blueprint blueprint) {
        if (!player.isOnline()) {
            return;
        }
        renderer.clear(player);
        BlueprintSession session = new BlueprintSession(blueprint);
        List<String> unknown = session.resolvePalette();
        sessions.start(player.getUniqueId(), session);
        Messages.info(player, String.format(
                "Loaded <white>%s</white> (%dx%dx%d, %d blocks). Look at the ground and run <aqua>/bp place</aqua>.",
                Messages.escape(blueprint.name()), blueprint.width(), blueprint.height(), blueprint.length(),
                blueprint.solidCount()));
        if (!unknown.isEmpty()) {
            Messages.error(player, unknown.size() + " block types are unknown to this server and render as stone.");
            plugin.getLogger().warning("Unknown blocks in " + blueprint.name() + ": " + String.join(", ", unknown));
        }
    }

    private void place(Player player) {
        Optional<BlueprintSession> session = requireSession(player);
        if (session.isEmpty()) {
            return;
        }
        Block target = player.getTargetBlockExact(8);
        Block origin = target != null ? target.getRelative(0, 1, 0) : player.getLocation().getBlock();
        session.get().place(player.getWorld(), origin.getX(), origin.getY(), origin.getZ());
        session.get().setVisible(true);
        refreshNow(player, session.get());
        Messages.info(player, String.format(
                "Placed at <white>%d %d %d</white>. Adjust with /bp move and /bp rotate.",
                origin.getX(), origin.getY(), origin.getZ()));
    }

    private void move(Player player, String[] args) {
        Optional<BlueprintSession> session = requirePlaced(player);
        if (session.isEmpty()) {
            return;
        }
        if (args.length < 4) {
            Messages.error(player, "Usage: /bp move <dx> <dy> <dz>");
            return;
        }
        try {
            session.get().moveBy(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        } catch (NumberFormatException e) {
            Messages.error(player, "dx, dy and dz must be whole numbers.");
            return;
        }
        refreshNow(player, session.get());
        Messages.info(player, "Moved.");
    }

    private void rotate(Player player, String[] args) {
        Optional<BlueprintSession> session = requirePlaced(player);
        if (session.isEmpty()) {
            return;
        }
        int degrees = 90;
        if (args.length >= 2) {
            try {
                degrees = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                degrees = 0;
            }
        }
        if (degrees % 90 != 0 || degrees == 0) {
            Messages.error(player, "Rotation must be a non-zero multiple of 90.");
            return;
        }
        session.get().rotate(Math.floorMod(degrees / 90, 4));
        refreshNow(player, session.get());
        Messages.info(player, "Rotated.");
    }

    private void layer(Player player, String[] args) {
        Optional<BlueprintSession> session = requirePlaced(player);
        if (session.isEmpty()) {
            return;
        }
        if (args.length < 2) {
            Messages.error(player, "Usage: /bp layer <n|all|up|down>");
            return;
        }
        BlueprintSession current = session.get();
        int target;
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "all" -> target = -1;
            case "up" -> target = Math.min(current.height() - 1, current.layer() + 1);
            case "down" -> target = current.layer() < 0 ? current.height() - 1 : Math.max(0, current.layer() - 1);
            default -> {
                try {
                    target = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    Messages.error(player, "Layer must be a number, all, up or down.");
                    return;
                }
                if (target < 0 || target >= current.height()) {
                    Messages.error(player, "Layer must be between 1 and " + current.height() + ".");
                    return;
                }
            }
        }
        current.setLayer(target);
        refreshNow(player, current);
        Messages.info(player, target < 0 ? "Showing all layers." : "Showing layer " + (target + 1) + "/" + current.height() + ".");
    }

    private void setVisible(Player player, boolean visible) {
        Optional<BlueprintSession> session = requireSession(player);
        if (session.isEmpty()) {
            return;
        }
        session.get().setVisible(visible);
        refreshNow(player, session.get());
        Messages.info(player, visible ? "Ghosts shown." : "Ghosts hidden.");
    }

    private void status(Player player) {
        Optional<BlueprintSession> session = requirePlaced(player);
        if (session.isEmpty()) {
            return;
        }
        ProgressScanner.Result result = scan(player, session.get());
        Messages.info(player, String.format(
                "<white>%s</white>: <green>%d%%</green> done - %d/%d correct, <aqua>%d missing</aqua>, <red>%d wrong</red>.",
                Messages.escape(session.get().blueprint().name()), result.percent(), result.correct(), result.total(),
                result.missing(), result.wrong()));
    }

    private void materials(Player player) {
        Optional<BlueprintSession> session = requirePlaced(player);
        if (session.isEmpty()) {
            return;
        }
        ProgressScanner.Result result = scan(player, session.get());
        if (result.remaining().isEmpty()) {
            Messages.info(player, "Nothing left to build.");
            return;
        }
        Messages.raw(player, "<dark_gray>--- <aqua>Materials</aqua> (remaining / total / in inventory) ---");
        result.remaining().entrySet().stream()
                .sorted(Map.Entry.<Material, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(MATERIAL_LINES)
                .forEach(entry -> {
                    Material material = entry.getKey();
                    int inventory = countInInventory(player, material);
                    String color = inventory >= entry.getValue() ? "green" : "red";
                    Messages.raw(player, String.format(
                            "<white>%s</white> <gray>x%d / %d / <%s>%d</%s>",
                            material.name().toLowerCase(Locale.ROOT), entry.getValue(),
                            result.needed().get(material), color, inventory, color));
                });
        int hidden = result.remaining().size() - MATERIAL_LINES;
        if (hidden > 0) {
            Messages.raw(player, "<gray>... and " + hidden + " more material types.");
        }
    }

    private ProgressScanner.Result scan(Player player, BlueprintSession session) {
        ProgressScanner scanner = new ProgressScanner(new BlockMatcher(plugin.settings().strictStates()));
        return scanner.scan(Bukkit.getWorld(session.worldId()), session);
    }

    private static int countInInventory(Player player, Material material) {
        int count = 0;
        for (var stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void clear(Player player) {
        renderer.clear(player);
        sessions.end(player.getUniqueId());
        Messages.info(player, "Blueprint cleared.");
    }

    private void reload(Player player) {
        if (!player.hasPermission("ghostblueprints.admin")) {
            Messages.error(player, "You do not have permission to do that.");
            return;
        }
        plugin.reload();
        Messages.info(player, "Configuration reloaded.");
    }

    private Optional<BlueprintSession> requireSession(Player player) {
        Optional<BlueprintSession> session = sessions.get(player.getUniqueId());
        if (session.isEmpty()) {
            Messages.error(player, "No blueprint loaded. Use /bp load <name>.");
        }
        return session;
    }

    private Optional<BlueprintSession> requirePlaced(Player player) {
        Optional<BlueprintSession> session = requireSession(player);
        if (session.isPresent() && !session.get().isPlaced()) {
            Messages.error(player, "Place the blueprint first with /bp place.");
            return Optional.empty();
        }
        return session;
    }

    private void refreshNow(Player player, BlueprintSession session) {
        renderer.refresh(player, session);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "load" -> {
                    try {
                        return filter(library.names(), args[1]);
                    } catch (IOException e) {
                        return List.of();
                    }
                }
                case "layer" -> {
                    return filter(List.of("all", "up", "down"), args[1]);
                }
                case "rotate" -> {
                    return filter(List.of("90", "180", "270"), args[1]);
                }
                default -> {
                    return List.of();
                }
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
