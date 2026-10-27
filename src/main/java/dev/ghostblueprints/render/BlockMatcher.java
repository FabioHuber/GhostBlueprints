package dev.ghostblueprints.render;

import org.bukkit.Material;
import org.bukkit.Tag;
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
        return actual.getMaterial().isAir() || isReplaceable(actual.getMaterial()) ? GhostKind.MISSING : GhostKind.WRONG;
    }

    private static boolean isReplaceable(Material material) {
        return material == Material.WATER || material == Material.LAVA || Tag.REPLACEABLE.isTagged(material);
    }
}
