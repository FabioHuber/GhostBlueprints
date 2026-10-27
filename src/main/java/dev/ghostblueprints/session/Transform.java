package dev.ghostblueprints.session;

/** Clockwise quarter-turn rotation of cell coordinates inside a width x length footprint. */
public final class Transform {
    private Transform() {
    }

    public static int x(int x, int z, int width, int length, int turns) {
        return switch (turns & 3) {
            case 1 -> length - 1 - z;
            case 2 -> width - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    public static int z(int x, int z, int width, int length, int turns) {
        return switch (turns & 3) {
            case 1 -> x;
            case 2 -> length - 1 - z;
            case 3 -> width - 1 - x;
            default -> z;
        };
    }
}
