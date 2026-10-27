package dev.ghostblueprints.render;

import dev.ghostblueprints.Settings;
import dev.ghostblueprints.schematic.Blueprint;
import dev.ghostblueprints.session.BlueprintSession;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Shows the blocks a player still has to build as client-side-only block displays.
 * Displays are spawned invisible by default and revealed to the owning player alone.
 */
public final class GhostRenderer {
    private static final float WRONG_SCALE = 1.002f;
    private static final float VIEW_RANGE = 1.0f;

    private final Plugin plugin;
    private final Supplier<Settings> settings;
    private final Map<UUID, Map<Long, Ghost>> ghostsByPlayer = new HashMap<>();

    public GhostRenderer(Plugin plugin, Supplier<Settings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    private record Ghost(BlockDisplay display, GhostKind kind, int paletteIndex) {
    }

    private record Candidate(long key, int x, int y, int z, int paletteIndex, GhostKind kind, double distanceSquared) {
    }

    public void refresh(Player player, BlueprintSession session) {
        World world = session.isPlaced() ? Bukkit.getWorld(session.worldId()) : null;
        if (world == null || !session.isVisible() || !player.getWorld().equals(world)) {
            clear(player);
            return;
        }

        Settings config = settings.get();
        List<Candidate> candidates = collectCandidates(player, session, world, config);
        Map<Long, Candidate> desired = new HashMap<>();
        for (Candidate candidate : candidates) {
            desired.put(candidate.key(), candidate);
        }

        Map<Long, Ghost> current = ghostsByPlayer.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        Iterator<Map.Entry<Long, Ghost>> existing = current.entrySet().iterator();
        while (existing.hasNext()) {
            Map.Entry<Long, Ghost> entry = existing.next();
            Candidate wanted = desired.get(entry.getKey());
            Ghost ghost = entry.getValue();
            boolean stale = wanted == null
                    || wanted.kind() != ghost.kind()
                    || wanted.paletteIndex() != ghost.paletteIndex()
                    || !ghost.display().isValid();
            if (stale) {
                ghost.display().remove();
                existing.remove();
            }
        }

        for (Candidate candidate : candidates) {
            if (!current.containsKey(candidate.key())) {
                BlockDisplay display = spawn(player, world, candidate, session.blockData(candidate.paletteIndex()), config);
                current.put(candidate.key(), new Ghost(display, candidate.kind(), candidate.paletteIndex()));
            }
        }

        if (config.actionBar()) {
            player.sendActionBar(actionBar(session, current.size()));
        }
    }

    public void clear(Player player) {
        clear(player.getUniqueId());
    }

    public void clear(UUID playerId) {
        Map<Long, Ghost> ghosts = ghostsByPlayer.remove(playerId);
        if (ghosts != null) {
            ghosts.values().forEach(ghost -> ghost.display().remove());
        }
    }

    public void clearAll() {
        ghostsByPlayer.values().forEach(ghosts -> ghosts.values().forEach(ghost -> ghost.display().remove()));
        ghostsByPlayer.clear();
    }

    private List<Candidate> collectCandidates(Player player, BlueprintSession session, World world, Settings config) {
        Location location = player.getLocation();
        int radius = config.renderRadius();
        int minX = Math.max(session.originX(), location.getBlockX() - radius);
        int maxX = Math.min(session.originX() + session.width() - 1, location.getBlockX() + radius);
        int minZ = Math.max(session.originZ(), location.getBlockZ() - radius);
        int maxZ = Math.min(session.originZ() + session.length() - 1, location.getBlockZ() + radius);
        int minY = Math.max(session.originY(), location.getBlockY() - radius);
        int maxY = Math.min(session.originY() + session.height() - 1, location.getBlockY() + radius);
        if (session.layer() >= 0) {
            minY = Math.max(minY, session.originY() + session.layer());
            maxY = Math.min(maxY, session.originY() + session.layer());
        }

        BlockMatcher matcher = new BlockMatcher(config.strictStates());
        List<Candidate> candidates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    Candidate candidate = candidateAt(session, world, matcher, location, x, y, z);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(Candidate::distanceSquared));
        return candidates.size() > config.maxGhosts() ? candidates.subList(0, config.maxGhosts()) : candidates;
    }

    private Candidate candidateAt(
            BlueprintSession session, World world, BlockMatcher matcher, Location viewer, int x, int y, int z) {
        int localX = x - session.originX();
        int localY = y - session.originY();
        int localZ = z - session.originZ();
        int index = session.paletteIndexAt(localX, localY, localZ);
        if (index == Blueprint.AIR) {
            return null;
        }
        if (session.layer() < 0 && session.isEnclosed(localX, localY, localZ)) {
            return null;
        }
        Block block = world.getBlockAt(x, y, z);
        GhostKind kind = matcher.classify(block, session.blockData(index));
        if (kind == null) {
            return null;
        }
        double dx = x + 0.5 - viewer.getX();
        double dy = y + 0.5 - viewer.getY();
        double dz = z + 0.5 - viewer.getZ();
        return new Candidate(key(x, y, z), x, y, z, index, kind, dx * dx + dy * dy + dz * dz);
    }

    private BlockDisplay spawn(Player player, World world, Candidate candidate, BlockData data, Settings config) {
        boolean wrong = candidate.kind() == GhostKind.WRONG;
        float scale = wrong ? WRONG_SCALE : config.missingScale();
        float offset = (1.0f - scale) / 2.0f;
        Transformation transformation = new Transformation(
                new Vector3f(offset, offset, offset), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf());

        BlockDisplay display = world.spawn(
                new Location(world, candidate.x(), candidate.y(), candidate.z()),
                BlockDisplay.class,
                entity -> {
                    entity.setVisibleByDefault(false);
                    entity.setPersistent(false);
                    entity.setBlock(data);
                    entity.setTransformation(transformation);
                    entity.setViewRange(VIEW_RANGE);
                    entity.setShadowRadius(0f);
                    entity.setGlowing(wrong || config.missingGlow());
                    entity.setGlowColorOverride(wrong ? Color.RED : Color.AQUA);
                });
        player.showEntity(plugin, display);
        return display;
    }

    private static Component actionBar(BlueprintSession session, int ghostCount) {
        String layer = session.layer() < 0 ? "all" : (session.layer() + 1) + "/" + session.height();
        return Component.text("Blueprint " + session.blueprint().name() + " | layer " + layer + " | " + ghostCount + " ghosts");
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFF);
    }
}
