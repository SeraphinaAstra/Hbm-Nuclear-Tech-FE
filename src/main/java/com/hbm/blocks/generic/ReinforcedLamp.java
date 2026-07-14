package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.radiation.ShieldingRegistry;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.interfaces.IRadShielding;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class ReinforcedLamp extends Block implements IRadResistantBlock, IRadShielding {

	private final boolean isOn;

	public ReinforcedLamp(Material materialIn, boolean b, String s) {
		super(materialIn);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		isOn = b;
		if(b){
			this.setLightLevel(1.0F);
		}

		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		if (!worldIn.isRemote)
        {
            if (this.isOn && !(worldIn.isBlockPowered(pos)))
            {
            	worldIn.scheduleUpdate(pos, this, 4);
            }
            else if (!this.isOn && worldIn.isBlockPowered(pos))
            {
            	worldIn.setBlockState(pos, ModBlocks.reinforced_lamp_on.getDefaultState(), 2);
            }
        }
        RadiationSystemNT.markSectionForRebuild(worldIn, pos);
	}

	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		if (!worldIn.isRemote)
        {
            if (this.isOn && !(worldIn.isBlockPowered(pos)))
            {
            	worldIn.scheduleUpdate(pos, this, 4);
            }
            else if (!this.isOn && worldIn.isBlockPowered(pos))
            {
            	worldIn.setBlockState(pos, ModBlocks.reinforced_lamp_on.getDefaultState(), 2);
            }
        }
	}

	@Override
	public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		if (!worldIn.isRemote && this.isOn && !(worldIn.isBlockPowered(pos)))
        {
			worldIn.setBlockState(pos, ModBlocks.reinforced_lamp_off.getDefaultState(), 2);
        }
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return Item.getItemFromBlock(ModBlocks.reinforced_lamp_off);
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return new ItemStack(ModBlocks.reinforced_lamp_off);
	}

    // ---- IRadShielding (new occlusion system) ----
    // Both on/off states have the same HVL (material is identical).
    @Override
    public double getHVLPerBlock(IBlockState state) {
        return ShieldingRegistry.getHVLDirect(this);
    }

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		float hardness = this.getExplosionResistance(null);
		tooltip.add("§2[Radiation Shielding]§r");
		String hvlLine = ShieldingRegistry.getHVLTooltipLine(this.getDefaultState());
		if (hvlLine != null) {
			tooltip.add("§2" + hvlLine);
		}
		if(hardness > 50){
			tooltip.add("§6Blast Resistance: "+hardness+"§r");
		}
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		RadiationSystemNT.markSectionForRebuild(worldIn, pos);
		super.breakBlock(worldIn, pos, state);
	}
}
