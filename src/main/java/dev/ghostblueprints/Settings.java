package dev.ghostblueprints;

import org.bukkit.configuration.file.FileConfiguration;

/** Immutable snapshot of config.yml, with values clamped to sane ranges. */
public record Settings(
        int renderRadius,
        int maxGhosts,
        int updateIntervalTicks,
        boolean strictStates,
        float missingScale,
        boolean missingGlow,
        boolean actionBar,
        int maxVolume,
        boolean importWorldEditFolders) {

    public static Settings from(FileConfiguration config) {
        return new Settings(
                clamp(config.getInt("render.radius", 16), 4, 64),
                clamp(config.getInt("render.max-ghosts", 1500), 50, 10_000),
                clamp(config.getInt("render.update-interval-ticks", 5), 1, 40),
                config.getString("match-mode", "MATERIAL").equalsIgnoreCase("STATE"),
                (float) Math.max(0.1, Math.min(1.0, config.getDouble("render.missing-scale", 0.6))),
                config.getBoolean("render.missing-glow", false),
                config.getBoolean("render.action-bar", true),
                clamp(config.getInt("limits.max-volume", 4_000_000), 1, 64_000_000),
                config.getBoolean("blueprints.import-worldedit-folders", true));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
