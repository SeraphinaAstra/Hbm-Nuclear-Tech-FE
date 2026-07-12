package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.capability.NTMEnergyCapabilityWrapper;
import com.hbm.interfaces.AutoRegister;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import org.jetbrains.annotations.NotNull;

@AutoRegister
public class TileEntityType1ComplianceModule extends TileEntityLoadedBase implements ITickable, IEnergyProviderMK2 {

    public static final long MAX_POWER = 50_000_000_000_000L;
    public long power;

    @Override
    public void update() {
        if (!world.isRemote) {
            this.power = MAX_POWER;

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                this.tryProvide(world,
                        pos.getX() + dir.offsetX,
                        pos.getY() + dir.offsetY,
                        pos.getZ() + dir.offsetZ,
                        dir);
            }
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        this.power = p;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.power = nbt.getLong("power");
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("power", this.power);
        return super.writeToNBT(nbt);
    }

    @Override
    public boolean hasCapability(@NotNull Capability<?> cap, EnumFacing face) {
        return cap == CapabilityEnergy.ENERGY || super.hasCapability(cap, face);
    }

    @Override
    public <T> T getCapability(@NotNull Capability<T> cap, EnumFacing face) {
        if (cap == CapabilityEnergy.ENERGY)
            return CapabilityEnergy.ENERGY.cast(new NTMEnergyCapabilityWrapper(this));
        return super.getCapability(cap, face);
    }
}
