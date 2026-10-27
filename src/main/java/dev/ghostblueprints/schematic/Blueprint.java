package dev.ghostblueprints.schematic;

/** Immutable, Bukkit-free block volume. Cells hold palette indices, or {@link #AIR}. */
public final class Blueprint {
    public static final int AIR = -1;

    private final String name;
    private final int width;
    private final int height;
    private final int length;
    private final String[] palette;
    private final int[] cells;
    private final int solidCount;

    public Blueprint(String name, int width, int height, int length, String[] palette, int[] cells) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.palette = palette;
        this.cells = cells;
        int solid = 0;
        for (int cell : cells) {
            if (cell != AIR) {
                solid++;
            }
        }
        this.solidCount = solid;
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int length() {
        return length;
    }

    public int solidCount() {
        return solidCount;
    }

    public int paletteSize() {
        return palette.length;
    }

    public String paletteEntry(int index) {
        return palette[index];
    }

    /** Palette index at the given cell, or {@link #AIR} for air and out-of-bounds cells. */
    public int paletteIndex(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) {
            return AIR;
        }
        return cells[(y * length + z) * width + x];
    }
}
