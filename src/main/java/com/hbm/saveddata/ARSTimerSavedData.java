package com.hbm.saveddata;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists ARS death timers per player UUID across log-out/log-in.
 * Follows the same WorldSavedData pattern as AuxSavedData.
 *
 * Data model:
 * - deathsTimerTicks: how many ticks remain before the player dies from ARS.
 *   0 = no timer active. Only meaningful when total dose >= 300 (committed threshold).
 * - Timer ticks down each server tick when the player is logged in.
 * - Survivable removal of a source (e.g. engineer removes fuel rods) can let
 *   the timer expire, at which point the player is pulled out of the committed
 *   band and reverts to natural decay behavior.
 */
public class ARSTimerSavedData extends WorldSavedData {

    private static final String DATA_NAME = "hbm_ars_timers";

    private final Map<UUID, Integer> deathsTimerTicks = new HashMap<>();

    public ARSTimerSavedData() {
        super(DATA_NAME);
    }

    public ARSTimerSavedData(String name) {
        super(name);
    }

    /// Public API

    public static ARSTimerSavedData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        ARSTimerSavedData instance = (ARSTimerSavedData) storage.getOrLoadData(ARSTimerSavedData.class, DATA_NAME);
        if (instance == null) {
            instance = new ARSTimerSavedData();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    /** Get remaining death timer ticks for a player, or 0 if none active. */
    public int getTimerTicks(UUID playerId) {
        return deathsTimerTicks.getOrDefault(playerId, 0);
    }

    /** Set (or clear, if 0) the death timer for a player. */
    public void setTimerTicks(UUID playerId, int ticks) {
        if (ticks <= 0) {
            deathsTimerTicks.remove(playerId);
        } else {
            deathsTimerTicks.put(playerId, ticks);
        }
        markDirty();
    }

    /** Extend an existing timer by additional ticks (additive). No-op if no timer active. */
    public void extendTimer(UUID playerId, int extraTicks) {
        int current = getTimerTicks(playerId);
        if (current > 0) {
            setTimerTicks(playerId, current + extraTicks);
        }
    }

    /** Tick down all active timers by 1 and return true if the timer expired this tick (player should die). */
    public boolean tick(UUID playerId) {
        int ticks = getTimerTicks(playerId);
        if (ticks <= 0) return false;
        ticks--;
        if (ticks <= 0) {
            setTimerTicks(playerId, 0);
            return true;
        }
        setTimerTicks(playerId, ticks);
        return false;
    }

    /** Cancel the timer for a player (e.g. rads fell below 300 naturally). */
    public void cancelTimer(UUID playerId) {
        setTimerTicks(playerId, 0);
    }

    // ---- NBT serialization (matching AuxSavedData pattern) ----

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        deathsTimerTicks.clear();
        int count = nbt.getInteger("timerCount");
        for (int i = 0; i < count; i++) {
            UUID uuid = UUID.fromString(nbt.getString("timerUuid_" + i));
            int ticks = nbt.getInteger("timerTicks_" + i);
            if (ticks > 0) {
                deathsTimerTicks.put(uuid, ticks);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("timerCount", deathsTimerTicks.size());
        int i = 0;
        for (Map.Entry<UUID, Integer> entry : deathsTimerTicks.entrySet()) {
            nbt.setString("timerUuid_" + i, entry.getKey().toString());
            nbt.setInteger("timerTicks_" + i, entry.getValue());
            i++;
        }
        return nbt;
    }
}