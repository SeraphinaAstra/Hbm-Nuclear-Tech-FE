package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.radiation.ShieldingRegistry;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.interfaces.IRadShielding;
import com.hbm.util.I18nUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class BlockRadResistant extends Block implements IRadResistantBlock, IRadShielding {

	public BlockRadResistant(Material materialIn, String s) {
		super(materialIn);
		this.setRegistryName(s);
		this.setTranslationKey(s);

		ModBlocks.ALL_BLOCKS.add(this);
    }

    // ---- IRadShielding (new occlusion system) ----
    // Fast path: all BlockRadResistant instances are state-independent.
    // Uses getHVLDirect to avoid any possibility of circular calls through
    // the public getHVLPerBlock(IBlockState) API.
    @Override
    public double getHVLPerBlock(IBlockState state) {
        return ShieldingRegistry.getHVLDirect(this);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        RadiationSystemNT.markSectionForRebuild(worldIn, pos);
        super.onBlockAdded(worldIn, pos, state);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        RadiationSystemNT.markSectionForRebuild(worldIn, pos);
        super.breakBlock(worldIn, pos, state);
    }

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		super.addInformation(stack, player, tooltip, advanced);
		tooltip.add("§2[" + I18nUtil.resolveKey("trait.radshield") + "]");
		String hvlLine = ShieldingRegistry.getHVLTooltipLine(this.getDefaultState());
		if (hvlLine != null) {
			tooltip.add("§2" + hvlLine);
		}
		float hardness = this.getExplosionResistance(null);
		if(hardness > 50){
			tooltip.add("§6" + I18nUtil.resolveKey("trait.blastres", hardness));
		}
	}
}
