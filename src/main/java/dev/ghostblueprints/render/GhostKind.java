package dev.ghostblueprints.render;

/** How a blueprint block differs from the world. Correct blocks have no kind. */
public enum GhostKind {
    /** The position is empty or replaceable. */
    MISSING,
    /** The position holds a different block. */
    WRONG
}
