package dev.ghostblueprints.listener;

import dev.ghostblueprints.render.GhostRenderer;
import dev.ghostblueprints.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/** Cleans up on quit and refreshes ghosts one tick after a player builds or breaks. */
public final class PlayerListener implements Listener {
    private final Plugin plugin;
    private final SessionManager sessions;
    private final GhostRenderer renderer;

    public PlayerListener(Plugin plugin, SessionManager sessions, GhostRenderer renderer) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.renderer = renderer;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        renderer.clear(event.getPlayer());
        sessions.end(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        refreshSoon(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        refreshSoon(event.getPlayer());
    }

    private void refreshSoon(Player player) {
        sessions.get(player.getUniqueId()).ifPresent(session ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        renderer.refresh(player, session);
                    }
                }));
    }
}
