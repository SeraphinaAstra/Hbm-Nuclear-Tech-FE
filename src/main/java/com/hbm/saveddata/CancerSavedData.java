package com.hbm.saveddata;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Persists stochastic cancer data per player UUID across log-out/log-in.
 * Follows the same WorldSavedData pattern as ARSTimerSavedData.
 *
 * Data model per player:
 *   monthlyDoseAccum  - RAD accumulated since month start; resets every 30 days
 *   monthStartTick    - world tick when the current monthly window started
 *   cancerBurden      - persistent debuff stack; only chemo pill reduces this
 */
public class CancerSavedData extends WorldSavedData {

    private static final String DATA_NAME = "hbm_cancer";

    private static final int TICKS_PER_MONTH = 30 * 24000; // 30 in-game days

    /**
     * Stochastic cancer risk model (spec §4.2a).
     *
     * Only EXCESS dose above the 10 RAD/month threshold feeds the roll:
     *   if (monthlyDoseAccum > 10) {
     *       excess = monthlyDoseAccum - 10;
     *       if (random < excess * CANCER_RISK_COEFFICIENT) cancerBurden += rollSeverity();
     *   }
     *
     * Both constants are UNRESOLVED placeholders pending a balancing pass
     * (same status as the ARS timer durations). They are intentionally
     * commented as tunable rather than presented as final numbers.
     */
    // Tunable: probability per excess-RAD that the month rolls a cancer event.
    private static final double CANCER_RISK_COEFFICIENT = 0.05D; // placeholder
    // Tunable: bias of the severity roll (higher = more burden per event).
    private static final double CANCER_SEVERITY_SCALE = 1.0D;    // placeholder

    private final Random rollRandom = new Random();

    public static class PlayerCancerData {
        public double monthlyDoseAccum = 0.0;
        public long monthStartTick = 0;
        public double cancerBurden = 0.0;
    }

    private final Map<UUID, PlayerCancerData> data = new HashMap<>();

    public CancerSavedData() {
        super(DATA_NAME);
    }

    public CancerSavedData(String name) {
        super(name);
    }

    public static CancerSavedData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        CancerSavedData instance = (CancerSavedData) storage.getOrLoadData(CancerSavedData.class, DATA_NAME);
        if (instance == null) {
            instance = new CancerSavedData();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    private PlayerCancerData getOrCreate(UUID playerId) {
        return data.computeIfAbsent(playerId, k -> new PlayerCancerData());
    }

    /** Accumulate dose for a player, rolling the month window if needed. */
    public void addDose(UUID playerId, double rads, long worldTime) {
        PlayerCancerData p = getOrCreate(playerId);
        p.monthlyDoseAccum += rads;

        // Roll month window if we crossed a boundary — evaluate the cancer
        // roll on the accumulated month BEFORE resetting it (spec §4.2a).
        if (worldTime - p.monthStartTick >= TICKS_PER_MONTH) {
            rollCancer(p);
            p.monthlyDoseAccum = 0.0;
            p.monthStartTick = worldTime;
        }
        markDirty();
    }

    /**
     * Stochastic cancer roll for a completed month (spec §4.2a).
     * Only excess above the 10 RAD/month threshold feeds the chance; chronic
     * exposure below threshold never accumulates risk.
     */
    private void rollCancer(PlayerCancerData p) {
        if (p.monthlyDoseAccum > 10.0D) {
            double excess = p.monthlyDoseAccum - 10.0D;
            if (rollRandom.nextDouble() < excess * CANCER_RISK_COEFFICIENT) {
                p.cancerBurden += rollSeverity();
            }
        }
    }

    /**
     * Severity of a single cancer roll. Placeholder curve: 1 + random in [0,1)
     * scaled by CANCER_SEVERITY_SCALE. Tunable during balancing pass.
     */
    private double rollSeverity() {
        return (1.0D + rollRandom.nextDouble()) * CANCER_SEVERITY_SCALE;
    }

    /** Current monthly accumulated dose, rolled if needed. */
    public double getMonthlyDose(UUID playerId, long worldTime) {
        PlayerCancerData p = getOrCreate(playerId);
        if (worldTime - p.monthStartTick >= TICKS_PER_MONTH) {
            p.monthlyDoseAccum = 0.0;
            p.monthStartTick = worldTime;
            markDirty();
        }
        return p.monthlyDoseAccum;
    }

    /** Reduce cancer burden by amount. */
    public void reduceCancerBurden(UUID playerId, double amount) {
        PlayerCancerData p = getOrCreate(playerId);
        p.cancerBurden = Math.max(0.0, p.cancerBurden - amount);
        markDirty();
    }

    /** Increase cancer burden by amount. */
    public void increaseCancerBurden(UUID playerId, double amount) {
        PlayerCancerData p = getOrCreate(playerId);
        p.cancerBurden += amount;
        markDirty();
    }

    /** Get current cancer burden. */
    public double getCancerBurden(UUID playerId) {
        return getOrCreate(playerId).cancerBurden;
    }

    /** Excess accumulated dose above the 10 RAD/month threshold (>= 0). */
    public double getMonthlyExcess(UUID playerId, long worldTime) {
        return Math.max(0.0D, getMonthlyDose(playerId, worldTime) - 10.0D);
    }

    /** Current month's probability (0..1) of a cancer roll, given accumulated dose. */
    public double getCurrentRollChance(UUID playerId, long worldTime) {
        return Math.min(1.0D, getMonthlyExcess(playerId, worldTime) * CANCER_RISK_COEFFICIENT);
    }

    /** In-game days remaining in the current monthly window (resets every 30 days). */
    public long getDaysLeftInMonth(UUID playerId, long worldTime) {
        PlayerCancerData p = getOrCreate(playerId);
        long elapsed = worldTime - p.monthStartTick;
        long remainingTicks = Math.max(0L, TICKS_PER_MONTH - elapsed);
        return remainingTicks / 24000L;
    }

    /** Severity tier of a given burden (0 = none/below, 1 = mild, 2 = moderate, 3 = severe, 4 = critical). */
    public static int burdenTier(double burden) {
        if (burden <= 0.0D) return 0;
        if (burden < 5) return 1;
        if (burden < 15) return 2;
        if (burden < 30) return 3;
        return 4;
    }


    // ---- NBT serialization (matching ARSTimerSavedData pattern) ----

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        data.clear();
        int count = nbt.getInteger("cancerCount");
        for (int i = 0; i < count; i++) {
            String uuidStr = nbt.getString("cancerUuid_" + i);
            PlayerCancerData p = new PlayerCancerData();
            p.monthlyDoseAccum = nbt.getDouble("cancerDose_" + i);
            p.monthStartTick = nbt.getLong("cancerMonthStart_" + i);
            p.cancerBurden = nbt.getDouble("cancerBurden_" + i);
            data.put(UUID.fromString(uuidStr), p);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("cancerCount", data.size());
        int i = 0;
        for (Map.Entry<UUID, PlayerCancerData> entry : data.entrySet()) {
            nbt.setString("cancerUuid_" + i, entry.getKey().toString());
            nbt.setDouble("cancerDose_" + i, entry.getValue().monthlyDoseAccum);
            nbt.setLong("cancerMonthStart_" + i, entry.getValue().monthStartTick);
            nbt.setDouble("cancerBurden_" + i, entry.getValue().cancerBurden);
            i++;
        }
        return nbt;
    }
}