package dev.ghostblueprints.render;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/** Compares world blocks with the expected blueprint block. */
public final class BlockMatcher {
    private final boolean strictStates;

    public BlockMatcher(boolean strictStates) {
        this.strictStates = strictStates;
    }

    /** Returns null when the world block already matches. */
    public GhostKind classify(Block block, BlockData expected) {
        BlockData actual = block.getBlockData();
        boolean matches = strictStates
                ? actual.matches(expected)
                : actual.getMaterial() == expected.getMaterial();
        if (matches) {
            return null;
        }
        return actual.getMaterial().isAir() || block.isReplaceable() ? GhostKind.MISSING : GhostKind.WRONG;
    }
}
