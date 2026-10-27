package dev.ghostblueprints.schematic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Reads Sponge schematics (.schem, versions 1-3) as written by WorldEdit and FAWE. */
public final class SchematicLoader {
    private SchematicLoader() {
    }

    public static Blueprint load(String name, Path file, int maxVolume) throws SchematicException {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(file)) {
            root = NbtReader.readCompressedRoot(in);
        } catch (IOException e) {
            throw new SchematicException("Could not read " + file.getFileName() + ": " + e.getMessage(), e);
        }
        return parse(name, root, maxVolume);
    }

    static Blueprint parse(String name, Map<String, Object> root, int maxVolume) throws SchematicException {
        if (root.get("Schematic") instanceof Map<?, ?> inner) {
            root = castCompound(inner);
        }
        int version = requireNumber(root, "Version");
        int width = requireNumber(root, "Width") & 0xFFFF;
        int height = requireNumber(root, "Height") & 0xFFFF;
        int length = requireNumber(root, "Length") & 0xFFFF;

        long volume = (long) width * height * length;
        if (volume == 0 || volume > maxVolume) {
            throw new SchematicException("Schematic volume " + volume + " is outside 1.." + maxVolume);
        }

        Map<String, Object> container = switch (version) {
            case 1, 2 -> root;
            case 3 -> requireCompound(root, "Blocks");
            default -> throw new SchematicException("Unsupported Sponge schematic version " + version);
        };
        String[] palette = readPalette(requireCompound(container, "Palette"));
        byte[] data = requireBytes(container, version == 3 ? "Data" : "BlockData");
        return new Blueprint(name, width, height, length, palette, decodeCells(data, palette, (int) volume));
    }

    private static String[] readPalette(Map<String, Object> tag) throws SchematicException {
        String[] palette = new String[tag.size()];
        for (Map.Entry<String, Object> entry : tag.entrySet()) {
            if (!(entry.getValue() instanceof Number id) || id.intValue() < 0 || id.intValue() >= palette.length) {
                throw new SchematicException("Invalid palette entry " + entry.getKey());
            }
            palette[id.intValue()] = entry.getKey();
        }
        for (String entry : palette) {
            if (entry == null) {
                throw new SchematicException("Palette ids are not contiguous");
            }
        }
        return palette;
    }

    private static int[] decodeCells(byte[] data, String[] palette, int volume) throws SchematicException {
        int[] cells = new int[volume];
        int pos = 0;
        for (int i = 0; i < volume; i++) {
            int value = 0;
            int shift = 0;
            byte next;
            do {
                if (pos >= data.length || shift > 28) {
                    throw new SchematicException("Block data is truncated or corrupt");
                }
                next = data[pos++];
                value |= (next & 0x7F) << shift;
                shift += 7;
            } while ((next & 0x80) != 0);
            if (value >= palette.length) {
                throw new SchematicException("Block data references unknown palette id " + value);
            }
            cells[i] = isAir(palette[value]) ? Blueprint.AIR : value;
        }
        return cells;
    }

    private static boolean isAir(String paletteEntry) {
        return paletteEntry.equals("minecraft:air")
                || paletteEntry.equals("minecraft:cave_air")
                || paletteEntry.equals("minecraft:void_air");
    }

    private static int requireNumber(Map<String, Object> tag, String key) throws SchematicException {
        if (tag.get(key) instanceof Number number) {
            return number.intValue();
        }
        throw new SchematicException("Missing field '" + key + "'; is this a Sponge .schem file?");
    }

    private static byte[] requireBytes(Map<String, Object> tag, String key) throws SchematicException {
        if (tag.get(key) instanceof byte[] bytes) {
            return bytes;
        }
        throw new SchematicException("Missing field '" + key + "'");
    }

    private static Map<String, Object> requireCompound(Map<String, Object> tag, String key)
            throws SchematicException {
        if (tag.get(key) instanceof Map<?, ?> map) {
            return castCompound(map);
        }
        throw new SchematicException("Missing field '" + key + "'");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castCompound(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
