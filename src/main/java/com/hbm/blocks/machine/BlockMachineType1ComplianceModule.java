package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityType1ComplianceModule;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockMachineType1ComplianceModule extends BlockMachineBase implements IEnergyConnectorBlock {

    public BlockMachineType1ComplianceModule(Material material, String name) {
        super(material, -1, name);
        this.setHardness(5.0F);
        this.setResistance(10.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityType1ComplianceModule();
    }

    @Override
    public boolean canConnect(IBlockAccess world, BlockPos pos, ForgeDirection dir) {
        return true;
    }
}
