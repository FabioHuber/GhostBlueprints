package dev.ghostblueprints;

import dev.ghostblueprints.command.BlueprintCommand;
import dev.ghostblueprints.listener.PlayerListener;
import dev.ghostblueprints.render.GhostRenderer;
import dev.ghostblueprints.session.SessionManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class GhostBlueprintsPlugin extends JavaPlugin {
    private volatile Settings settings;
    private SessionManager sessions;
    private GhostRenderer renderer;
    private BukkitTask refreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.from(getConfig());

        Path blueprintFolder = getDataFolder().toPath().resolve("blueprints");
        try {
            Files.createDirectories(blueprintFolder);
        } catch (IOException e) {
            getLogger().severe("Cannot create " + blueprintFolder + ": " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        sessions = new SessionManager();
        renderer = new GhostRenderer(this, this::settings);
        BlueprintLibrary library = new BlueprintLibrary(blueprintFolder, getDataFolder().toPath().getParent(), this::settings);

        BlueprintCommand command = new BlueprintCommand(this, sessions, renderer, library);
        Objects.requireNonNull(getCommand("blueprint")).setExecutor(command);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, sessions, renderer), this);
        scheduleRefresh();
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (renderer != null) {
            renderer.clearAll();
        }
    }

    public Settings settings() {
        return settings;
    }

    public void reload() {
        reloadConfig();
        settings = Settings.from(getConfig());
        scheduleRefresh();
    }

    private void scheduleRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        long interval = settings.updateIntervalTicks();
        refreshTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshAll, interval, interval);
    }

    private void refreshAll() {
        for (var entry : sessions.all()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                renderer.clear(entry.getKey());
                sessions.end(entry.getKey());
                continue;
            }
            renderer.refresh(player, entry.getValue());
        }
    }
}
