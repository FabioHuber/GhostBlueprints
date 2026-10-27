package dev.ghostblueprints.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchematicLoaderTest {
    @TempDir
    Path folder;

    @Test
    void loadsSpongeV2WithAirAndPalette() throws Exception {
        // 2x1x2: stone, air / air, oak_stairs
        Path file = writeV2(2, 1, 2, new String[] {"minecraft:air", "minecraft:stone", "minecraft:oak_stairs[facing=north]"},
                new byte[] {1, 0, 0, 2});

        Blueprint blueprint = SchematicLoader.load("test", file, 1000);

        assertEquals(2, blueprint.width());
        assertEquals(1, blueprint.height());
        assertEquals(2, blueprint.length());
        assertEquals(2, blueprint.solidCount());
        assertEquals("minecraft:stone", blueprint.paletteEntry(blueprint.paletteIndex(0, 0, 0)));
        assertEquals(Blueprint.AIR, blueprint.paletteIndex(1, 0, 0));
        assertEquals("minecraft:oak_stairs[facing=north]", blueprint.paletteEntry(blueprint.paletteIndex(1, 0, 1)));
    }

    @Test
    void rejectsVolumeAboveLimit() throws Exception {
        Path file = writeV2(2, 1, 2, new String[] {"minecraft:air", "minecraft:stone"}, new byte[] {1, 1, 1, 1});

        assertThrows(SchematicException.class, () -> SchematicLoader.load("test", file, 3));
    }

    @Test
    void rejectsTruncatedBlockData() throws Exception {
        Path file = writeV2(2, 1, 2, new String[] {"minecraft:air", "minecraft:stone"}, new byte[] {1, 1});

        assertThrows(SchematicException.class, () -> SchematicLoader.load("test", file, 1000));
    }

    @Test
    void rejectsUnknownPaletteId() throws Exception {
        Path file = writeV2(1, 1, 1, new String[] {"minecraft:air"}, new byte[] {5});

        assertThrows(SchematicException.class, () -> SchematicLoader.load("test", file, 1000));
    }

    private Path writeV2(int width, int height, int length, String[] palette, byte[] blockData) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(10);
            out.writeUTF("Schematic");
            intTag(out, "Version", 2);
            shortTag(out, "Width", width);
            shortTag(out, "Height", height);
            shortTag(out, "Length", length);
            out.writeByte(10);
            out.writeUTF("Palette");
            for (int i = 0; i < palette.length; i++) {
                intTag(out, palette[i], i);
            }
            out.writeByte(0);
            out.writeByte(7);
            out.writeUTF("BlockData");
            out.writeInt(blockData.length);
            out.write(blockData);
            out.writeByte(0);
        }
        Path file = folder.resolve("test.schem");
        Files.write(file, bytes.toByteArray());
        return file;
    }

    private static void intTag(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(3);
        out.writeUTF(name);
        out.writeInt(value);
    }

    private static void shortTag(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(2);
        out.writeUTF(name);
        out.writeShort(value);
    }
}
