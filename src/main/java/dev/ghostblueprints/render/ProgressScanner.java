package dev.ghostblueprints.render;

import dev.ghostblueprints.schematic.Blueprint;
import dev.ghostblueprints.session.BlueprintSession;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/** Full scan of a placed blueprint against the world. Unloaded chunks count as missing. */
public final class ProgressScanner {
    private final BlockMatcher matcher;

    public ProgressScanner(BlockMatcher matcher) {
        this.matcher = matcher;
    }

    public record Result(
            int total,
            int correct,
            int missing,
            int wrong,
            Map<Material, Integer> needed,
            Map<Material, Integer> remaining) {

        public int percent() {
            return total == 0 ? 100 : (int) (100L * correct / total);
        }
    }

    public Result scan(World world, BlueprintSession session) {
        Map<Material, Integer> needed = new EnumMap<>(Material.class);
        Map<Material, Integer> remaining = new EnumMap<>(Material.class);
        int total = 0;
        int correct = 0;
        int missing = 0;
        int wrong = 0;

        for (int y = 0; y < session.height(); y++) {
            for (int z = 0; z < session.length(); z++) {
                for (int x = 0; x < session.width(); x++) {
                    int index = session.paletteIndexAt(x, y, z);
                    if (index == Blueprint.AIR) {
                        continue;
                    }
                    BlockData expected = session.blockData(index);
                    Material item = expected.getPlacementMaterial();
                    needed.merge(item, 1, Integer::sum);
                    total++;

                    int worldX = session.originX() + x;
                    int worldY = session.originY() + y;
                    int worldZ = session.originZ() + z;
                    GhostKind kind = world.isChunkLoaded(worldX >> 4, worldZ >> 4)
                            ? matcher.classify(world.getBlockAt(worldX, worldY, worldZ), expected)
                            : GhostKind.MISSING;
                    if (kind == null) {
                        correct++;
                        continue;
                    }
                    remaining.merge(item, 1, Integer::sum);
                    if (kind == GhostKind.MISSING) {
                        missing++;
                    } else {
                        wrong++;
                    }
                }
            }
        }
        return new Result(total, correct, missing, wrong, needed, remaining);
    }
}
