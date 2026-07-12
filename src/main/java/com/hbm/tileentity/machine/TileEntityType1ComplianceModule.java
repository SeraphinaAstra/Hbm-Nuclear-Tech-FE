package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.capability.NTMEnergyCapabilityWrapper;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.interfaces.AutoRegister;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.render.amlfrom1710.Vec3;
import com.hbm.sound.AudioWrapper;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.ContaminationUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import org.jetbrains.annotations.NotNull;

@AutoRegister
public class TileEntityType1ComplianceModule extends TileEntityLoadedBase implements ITickable, IEnergyProviderMK2 {

    public static final long MAX_POWER = 50_000_000_000_000L;
    public long power;
    private AudioWrapper audio;

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

            radiate();
        } else {
            if (audio == null) {
                audio = createAudioLoop();
                audio.startSound();
            } else if (!audio.isPlaying()) {
                audio = rebootAudio(audio);
            }
            audio.keepAlive();
        }
    }

    private void radiate() {
        ChunkRadiationManager.proxy.incrementRad(world, pos, 1000F, 50000F);

        double range = 50D;
        float rads = 500000F;

        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                 pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).grow(range, range, range));

        for (EntityLivingBase e : entities) {
            Vec3 vec = Vec3.createVectorHelper(e.posX - (pos.getX() + 0.5),
                                               (e.posY + e.getEyeHeight()) - (pos.getY() + 0.5),
                                               e.posZ - (pos.getZ() + 0.5));
            double len = vec.length();
            vec = vec.normalize();

            float res = 0;
            for (int i = 1; i < len; i++) {
                int ix = (int) Math.floor(pos.getX() + 0.5 + vec.xCoord * i);
                int iy = (int) Math.floor(pos.getY() + 0.5 + vec.yCoord * i);
                int iz = (int) Math.floor(pos.getZ() + 0.5 + vec.zCoord * i);
                res += world.getBlockState(new BlockPos(ix, iy, iz)).getBlock().getExplosionResistance(null);
            }
            if (res < 1) res = 1;

            float eRads = rads;
            eRads /= (float) res;
            eRads /= (float) (len * len);

            ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, eRads);
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return MainRegistry.proxy.getLoopedSound(
            HBMSoundHandler.fensuHum, SoundCategory.BLOCKS,
            pos.getX(), pos.getY(), pos.getZ(),
            getVolume(1.0F), 25F, 1.0F, 20);
    }

    @Override
    public void onChunkUnload() {
        if (audio != null) { audio.stopSound(); audio = null; }
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        if (audio != null) { audio.stopSound(); audio = null; }
        super.invalidate();
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