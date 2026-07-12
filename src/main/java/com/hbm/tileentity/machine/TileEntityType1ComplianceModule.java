package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.interfaces.AutoRegister;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

@AutoRegister
public class TileEntityType1ComplianceModule extends TileEntityLoadedBase implements ITickable, IEnergyProviderMK2 {

    private static final long POWER_PER_TICK = 50_000_000_000_001L;
    private static final float RADIATION_PER_TICK = 100F;
    private static final float RADIATION_MAX = 5000F;

    private AudioWrapper audio;

    @Override
    public void update() {
        if (world == null) {
            return;
        }

        if (!world.isRemote) {
            ChunkRadiationManager.proxy.incrementRad(world, pos, RADIATION_PER_TICK, RADIATION_MAX);

            Nodespace.PowerNode node = Nodespace.getNode(world, pos);
            if (node == null || node.net == null) {
                node = new Nodespace.PowerNode(pos);
                Nodespace.createNode(world, node);
            }

            if (node != null && node.net != null) {
                node.net.sendPowerDiode(POWER_PER_TICK, false);
            }
        } else {
            if (MainRegistry.proxy.me() != null && MainRegistry.proxy.me().getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 30 * 30) {
                if (audio == null) {
                    audio = createAudioLoop();
                    audio.startSound();
                } else if (!audio.isPlaying()) {
                    audio = rebootAudio(audio);
                }

                audio.updateVolume(getVolume(0.5F));
                audio.keepAlive();
            } else if (audio != null) {
                audio.stopSound();
                audio = null;
            }
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return MainRegistry.proxy.getLoopedSound(HBMSoundHandler.electricHum, SoundCategory.BLOCKS, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 0.35F, 15F, 0.85F, 20);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void setPower(long power) {
        // This module provides a fixed output and does not store power.
    }

    @Override
    public long getPower() {
        return POWER_PER_TICK;
    }

    @Override
    public long getMaxPower() {
        return POWER_PER_TICK;
    }
}
