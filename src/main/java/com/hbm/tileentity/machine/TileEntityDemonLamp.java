package com.hbm.tileentity.machine;

import com.hbm.handler.radiation.RadiationOcclusion;
import com.hbm.interfaces.AutoRegister;
import com.hbm.render.amlfrom1710.Vec3;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@AutoRegister
public class TileEntityDemonLamp extends TileEntity implements ITickable {
	private AxisAlignedBB bb;

	@Override
	public void update(){
		if(!world.isRemote) {
			// Discrete point-source emission: register with the new occlusion system.
			// The old ad-hoc ray walk using getExplosionResistance() is replaced
			// by ShieldingRegistry HVL values via RadiationOcclusion.
			// Dose application happens in EntityEffectHandler.handleRadiation()
			// which calls RadiationOcclusion.getDoseForEntity().
			// The instant-damage radius (< 2 blocks) is kept for point-blank
			// lethality. Strength = 100000F * 20 (converted to per-tick equivalent,
			// matching the old ~100000 RAD/s at range 1).
			RadiationOcclusion.registerSource(world, new RadiationOcclusion.RadiationSource(pos, 2000000D, 0.5D));
			// Short-range lethal damage (kept as-is — combat damage, not occluded dose)
			List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2, pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3));
			for(EntityLivingBase e : entities) {
				double dist = e.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
				if(dist < 2) {
					e.attackEntityFrom(DamageSource.IN_FIRE, 100);
				}
			}
		}
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox(){
		if (bb == null) bb = new AxisAlignedBB(pos.getX() - 16, pos.getY() - 1, pos.getZ() - 16, pos.getX() + 17, pos.getY() + 2, pos.getZ() + 17);
		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared(){
		return 65536.0D;
	}
}
