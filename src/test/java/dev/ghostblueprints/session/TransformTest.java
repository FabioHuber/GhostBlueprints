package dev.ghostblueprints.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransformTest {
    @Test
    void fourTurnsReturnToStart() {
        int width = 5;
        int length = 3;
        int x = 4;
        int z = 1;
        int turned = x;
        int turnedZ = z;
        int w = width;
        int l = length;
        for (int i = 0; i < 4; i++) {
            int nextX = Transform.x(turned, turnedZ, w, l, 1);
            turnedZ = Transform.z(turned, turnedZ, w, l, 1);
            turned = nextX;
            int swap = w;
            w = l;
            l = swap;
        }
        assertEquals(x, turned);
        assertEquals(z, turnedZ);
    }

    @Test
    void clockwiseTurnMovesNorthEdgeToEast() {
        // Cell on the north edge (z = 0) of a 4x2 footprint ends on the east edge (x = length - 1).
        assertEquals(1, Transform.x(2, 0, 4, 2, 1));
        assertEquals(2, Transform.z(2, 0, 4, 2, 1));
    }

    @Test
    void inverseTurnsUndoRotation() {
        int width = 6;
        int length = 2;
        int rx = Transform.x(5, 1, width, length, 3);
        int rz = Transform.z(5, 1, width, length, 3);
        // After 270 degrees the footprint is length x width; undo with one more quarter turn.
        assertEquals(5, Transform.x(rx, rz, length, width, 1));
        assertEquals(1, Transform.z(rx, rz, length, width, 1));
    }
}
