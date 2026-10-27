package dev.ghostblueprints.session;

import dev.ghostblueprints.schematic.Blueprint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.StructureRotation;

/** One player's active blueprint: where it is placed, how it is rotated and which layer is shown. */
public final class BlueprintSession {
    private static final StructureRotation[] ROTATIONS = {
        StructureRotation.NONE,
        StructureRotation.CLOCKWISE_90,
        StructureRotation.CLOCKWISE_180,
        StructureRotation.COUNTERCLOCKWISE_90
    };

    private final Blueprint blueprint;
    private final BlockData[][] paletteByTurns = new BlockData[4][];

    private UUID worldId;
    private int originX;
    private int originY;
    private int originZ;
    private int turns;
    private int layer = -1;
    private boolean visible = true;

    public BlueprintSession(Blueprint blueprint) {
        this.blueprint = blueprint;
    }

    public Blueprint blueprint() {
        return blueprint;
    }

    /**
     * Resolves palette entries to block data. Must run on the main thread.
     *
     * @return palette entries this server version does not know; they render as stone
     */
    public List<String> resolvePalette() {
        List<String> unknown = new ArrayList<>();
        BlockData[] base = new BlockData[blueprint.paletteSize()];
        for (int i = 0; i < base.length; i++) {
            try {
                base[i] = Bukkit.createBlockData(blueprint.paletteEntry(i));
            } catch (IllegalArgumentException e) {
                unknown.add(blueprint.paletteEntry(i));
                base[i] = Bukkit.createBlockData(Material.STONE);
            }
        }
        paletteByTurns[0] = base;
        return unknown;
    }

    public BlockData blockData(int paletteIndex) {
        BlockData[] palette = paletteByTurns[turns];
        if (palette == null) {
            palette = new BlockData[paletteByTurns[0].length];
            for (int i = 0; i < palette.length; i++) {
                BlockData rotated = paletteByTurns[0][i].clone();
                rotated.rotate(ROTATIONS[turns]);
                palette[i] = rotated;
            }
            paletteByTurns[turns] = palette;
        }
        return palette[paletteIndex];
    }

    public void place(World world, int x, int y, int z) {
        worldId = world.getUID();
        originX = x;
        originY = y;
        originZ = z;
    }

    public boolean isPlaced() {
        return worldId != null;
    }

    public UUID worldId() {
        return worldId;
    }

    public void moveBy(int dx, int dy, int dz) {
        originX += dx;
        originY += dy;
        originZ += dz;
    }

    public void rotate(int quarterTurns) {
        turns = (turns + quarterTurns) & 3;
    }

    public int originX() {
        return originX;
    }

    public int originY() {
        return originY;
    }

    public int originZ() {
        return originZ;
    }

    public int width() {
        return (turns & 1) == 0 ? blueprint.width() : blueprint.length();
    }

    public int length() {
        return (turns & 1) == 0 ? blueprint.length() : blueprint.width();
    }

    public int height() {
        return blueprint.height();
    }

    /** Zero-based layer shown on its own, or -1 when all layers are shown. */
    public int layer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /** Palette index at a cell of the placed (rotated) blueprint, relative to its origin. */
    public int paletteIndexAt(int x, int y, int z) {
        if (x < 0 || z < 0 || x >= width() || z >= length()) {
            return Blueprint.AIR;
        }
        int back = (4 - turns) & 3;
        int sourceX = Transform.x(x, z, width(), length(), back);
        int sourceZ = Transform.z(x, z, width(), length(), back);
        return blueprint.paletteIndex(sourceX, y, sourceZ);
    }

    /** True when all six neighbours are solid, so the block can never be seen from outside. */
    public boolean isEnclosed(int x, int y, int z) {
        return paletteIndexAt(x + 1, y, z) != Blueprint.AIR
                && paletteIndexAt(x - 1, y, z) != Blueprint.AIR
                && paletteIndexAt(x, y + 1, z) != Blueprint.AIR
                && paletteIndexAt(x, y - 1, z) != Blueprint.AIR
                && paletteIndexAt(x, y, z + 1) != Blueprint.AIR
                && paletteIndexAt(x, y, z - 1) != Blueprint.AIR;
    }
}
