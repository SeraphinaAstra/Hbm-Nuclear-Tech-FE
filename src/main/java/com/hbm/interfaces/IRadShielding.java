package com.hbm.interfaces;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Directional radiation occlusion shielding value, in Half-Value Layers (HVL) per block.
 * This is separate from {@link IRadResistantBlock}, which governs pocket-diffusion
 * flood-fill segmentation for the legacy ambient/biome radiation system. A block may
 * implement both interfaces independently — one is not a substitute for the other.
 *
 * Transmission fraction through a single block of this material is 0.5^HVL.
 * See ShieldingRegistry for the canonical material HVL table.
 */
public interface IRadShielding {

    /**
     * Fast path: called for the overwhelming majority of blocks traversed by a
     * radiation ray. Must not have side effects and must not query tile entities.
     *
     * @return HVL (half-value layers) contributed by one full block of this material.
     */
    double getHVLPerBlock(IBlockState state);

    /**
     * Override only for state-dependent shielding (e.g. doors/hatches/vaults that
     * only shield while closed). Default falls through to the cheap fast path.
     * The raycast engine always calls this overload; only override if you actually
     * need World/BlockPos to look up a tile entity's state.
     */
    default double getHVLPerBlock(World world, BlockPos pos, IBlockState state) {
        return getHVLPerBlock(state);
    }
}
