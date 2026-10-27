package dev.ghostblueprints.schematic;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Minimal reader for gzip-compressed NBT. Compounds become maps, lists become lists. */
final class NbtReader {
    private static final int TAG_END = 0;
    private static final int TAG_COMPOUND = 10;
    private static final int MAX_DEPTH = 32;
    private static final int MAX_ARRAY_LENGTH = 1 << 26;

    private final DataInputStream in;

    private NbtReader(DataInputStream in) {
        this.in = in;
    }

    static Map<String, Object> readCompressedRoot(InputStream raw) throws IOException {
        try (var stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            return new NbtReader(stream).readRoot();
        }
    }

    private Map<String, Object> readRoot() throws IOException {
        if (in.readUnsignedByte() != TAG_COMPOUND) {
            throw new IOException("Root tag is not a compound");
        }
        in.readUTF();
        return readCompound(0);
    }

    private Map<String, Object> readCompound(int depth) throws IOException {
        checkDepth(depth);
        Map<String, Object> map = new HashMap<>();
        int type;
        while ((type = in.readUnsignedByte()) != TAG_END) {
            String name = in.readUTF();
            map.put(name, readPayload(type, depth + 1));
        }
        return map;
    }

    private Object readPayload(int type, int depth) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> readByteArray();
            case 8 -> in.readUTF();
            case 9 -> readList(depth);
            case TAG_COMPOUND -> readCompound(depth);
            case 11 -> readIntArray();
            case 12 -> readLongArray();
            default -> throw new IOException("Unknown NBT tag type " + type);
        };
    }

    private List<Object> readList(int depth) throws IOException {
        checkDepth(depth);
        int elementType = in.readUnsignedByte();
        int length = checkLength(in.readInt());
        if (length > 0 && elementType == TAG_END) {
            throw new IOException("Non-empty NBT list of type END");
        }
        List<Object> list = new ArrayList<>(Math.min(length, 1024));
        for (int i = 0; i < length; i++) {
            list.add(readPayload(elementType, depth + 1));
        }
        return list;
    }

    private byte[] readByteArray() throws IOException {
        byte[] bytes = new byte[checkLength(in.readInt())];
        in.readFully(bytes);
        return bytes;
    }

    private int[] readIntArray() throws IOException {
        int[] ints = new int[checkLength(in.readInt())];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = in.readInt();
        }
        return ints;
    }

    private long[] readLongArray() throws IOException {
        long[] longs = new long[checkLength(in.readInt())];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = in.readLong();
        }
        return longs;
    }

    private static int checkLength(int length) throws IOException {
        if (length < 0 || length > MAX_ARRAY_LENGTH) {
            throw new IOException("Invalid NBT array length " + length);
        }
        return length;
    }

    private static void checkDepth(int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("NBT nesting too deep");
        }
    }
}
