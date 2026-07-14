package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.handler.radiation.RadiationOcclusion;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class YellowBarrel extends BaseBarrel {

	Random rand = new Random();

	public YellowBarrel(Material materialIn, String s) {
		super(materialIn);
		this.setTranslationKey(s);
		this.setRegistryName(s);

		ModBlocks.ALL_BLOCKS.add(this);
	}

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.CENTER_BIG;
    }

	@Override
	public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn) {
		if (!worldIn.isRemote && worldIn instanceof WorldServer) {
			((WorldServer)worldIn).addScheduledTask(() -> {
        		explode(worldIn, pos.getX(), pos.getY(), pos.getZ());
        	});
        }
	}

	public void explode(World p_149695_1_, int x, int y, int z) {
		// Barrel is destroyed — drop its registered radiation source.
		RadiationOcclusion.deregisterSource(p_149695_1_, new BlockPos(x, y, z));
		if(rand.nextInt(3) == 0) {
			p_149695_1_.setBlockState(new BlockPos(x, y, z), ModBlocks.toxic_block.getDefaultState());
		} else {
			p_149695_1_.createExplosion(null, x, y, z, 18.0F, true);
		}
    	ExplosionNukeGeneric.waste(p_149695_1_, x, y, z, 35);

        ChunkRadiationManager.proxy.incrementRad(p_149695_1_, new BlockPos(x, y, z), 35F, 1500F);
    }

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
		super.updateTick(world, pos, state, rand);

		// Discrete point-source emission: register with the occlusion system
		// each tick (idempotent keyed by pos). Old continuous
		// chunk-rad call removed. Strength mirrors the old rad value;
		// tuning pending balancing pass.
		if(this == ModBlocks.yellow_barrel){
            RadiationOcclusion.registerSource(world, new RadiationOcclusion.RadiationSource(pos, 5));
        } else {
            RadiationOcclusion.registerSource(world, new RadiationOcclusion.RadiationSource(pos, 0.5));
        }

        world.scheduleUpdate(pos, this, this.tickRate(world));
	}

	@Override
	public int tickRate(World worldIn) {
		return 20;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		super.randomDisplayTick(stateIn, worldIn, pos, rand);
		worldIn.spawnParticle(EnumParticleTypes.TOWN_AURA, pos.getX() + rand.nextFloat() * 0.5F + 0.25F, pos.getY() + 1.1F, pos.getZ() + rand.nextFloat() * 0.5F + 0.25F, 0.0D, 0.0D, 0.0D);
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		super.onBlockAdded(worldIn, pos, state);
		worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
	}

}
