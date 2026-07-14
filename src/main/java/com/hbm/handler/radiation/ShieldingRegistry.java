package com.hbm.handler.radiation;

import java.util.HashMap;
import java.util.Map;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.IRadShielding;
import com.hbm.util.I18nUtil;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Single source of truth for radiation occlusion shielding values (HVL per block).
 * Consumed by both the raycast occlusion engine and block tooltips.
 *
 * Two lookup paths:
 * - Blocks implementing {@link IRadShielding} directly (state-dependent shielding,
 *   e.g. doors that only shield while closed) are queried via the interface.
 * - Everything else is looked up in the static HVL map below, populated by initDefault().
 *   Blocks not present in the map and not implementing IRadShielding return 0 HVL —
 *   only explicitly registered materials shield radiation, full stop, no fallback
 *   to explosion resistance or any other heuristic.
 *
 * Anchor: Australium = 2.0 HVL/block (best native shielding material, derived from
 * being the densest material in NTM). All other materials scale as:
 *     HVL_per_block(material) = 2.0 * (density_material / density_australium)
 * with density_australium ~= 58.3 g/cm^3.
 */
public final class ShieldingRegistry {

    private static final Map<Block, Double> HVL_BY_BLOCK = new HashMap<>();

    private ShieldingRegistry() {}

    public static void registerShielding(Block block, double hvlPerBlock) {
        HVL_BY_BLOCK.put(block, hvlPerBlock);
    }

    /** Fast path, no tile entity lookup. Use for the common (non-door) case. */
    public static double getHVLPerBlock(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof IRadShielding) {
            return ((IRadShielding) block).getHVLPerBlock(state);
        }
        Double v = HVL_BY_BLOCK.get(block);
        return v != null ? v : 0.0D;
    }

    /** Full path — required for state-dependent blocks (doors/hatches/vaults). */
    public static double getHVLPerBlock(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof IRadShielding) {
            return ((IRadShielding) block).getHVLPerBlock(world, pos, state);
        }
        Double v = HVL_BY_BLOCK.get(block);
        return v != null ? v : 0.0D;
    }

    /**
     * Convenience for tooltips: fraction of radiation blocked by one full block
     * of this material, i.e. 1 - 0.5^HVL.
     */
    public static double getPercentBlockedPerBlock(IBlockState state) {
        double hvl = getHVLPerBlock(state);
        return 1.0D - Math.pow(0.5D, hvl);
    }

    /**
     * Returns a ready-to-append, localized tooltip line showing percent radiation
     * blocked per block (e.g. "Blocks 75.0% radiation per block"), or null if this
     * block/state has no registered shielding value. Callers should skip adding
     * anything to the tooltip list when this returns null rather than appending
     * an empty/zero line.
     */
    public static String getHVLTooltipLine(IBlockState state) {
        double hvl = getHVLPerBlock(state);
        if (hvl <= 0.0D) return null;
        double percent = getPercentBlockedPerBlock(state) * 100.0D;
        return I18nUtil.resolveKey("trait.radshield.hvl", String.format("%.1f", percent));
    }

    public static void initDefault() {
        // Locked material table — see NTM_FE_Radiation_Overhaul_Spec.md section 1.2
        registerShielding(ModBlocks.block_australium, 2.00D);   // densest material in NTM, ceiling value
        registerShielding(ModBlocks.block_lead, 0.39D);
        registerShielding(ModBlocks.block_steel, 0.27D);
        registerShielding(ModBlocks.block_desh, 0.186D);        // weaker than steel/lead — intentional, Desh isn't a shielding-purpose alloy despite being high-tech
        registerShielding(ModBlocks.brick_concrete, 0.08D);
        registerShielding(ModBlocks.brick_concrete_mossy, 0.08D);
        // brick_concrete_cracked / brick_concrete_broken intentionally NOT registered (0 HVL) —
        // they were never IRadResistantBlock in the legacy system either; damaged concrete doesn't shield.
    }
}
