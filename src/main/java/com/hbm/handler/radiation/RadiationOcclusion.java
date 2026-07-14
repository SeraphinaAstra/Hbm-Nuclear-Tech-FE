package com.hbm.handler.radiation;

import com.hbm.config.RadiationConfig;
import com.hbm.render.amlfrom1710.Vec3;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Directional occlusion (raycasting) engine for discrete point radiation sources.
 *
 * Replaces the legacy pocket-diffusion flood-fill for genuine gameplay-relevant
 * point sources (reactors, radioactive items, explosions-adjacent emitters). See
 * NTM_FE_Radiation_Overhaul_Spec.md Part A sections 2 and 6.
 *
 * Design notes (deliberately pragmatic, matching ContaminationUtil.radiate()):
 * - Ray walk is a simple integer-step march along a normalized direction vector,
 *   NOT a full Amanatides-Woo DDA voxel traversal. This codebase's precedent is
 *   intentionally simple; geometric partial-block sampling is descoped (flat
 *   multipliers for slabs only, handled in ShieldingRegistry consumers).
 * - Per step, the HVL/block is fetched from ShieldingRegistry (3-arg overload,
 *   so door/hatch/vault state is resolved correctly) and accumulated into an
 *   optical depth. transmitted = 0.5^opticalDepth.
 * - Final intensity for a source: (strength / distance^2) * transmitted, with a
 *   small epsilon floor on distance to avoid blowup at point-blank range.
 *
 * Caching (spec section 2.4):
 * - Cache keyed by (sourcePos, targetEntityId).
 * - Primary invalidation: recompute when source or target crosses a block
 *   boundary (compared each tick).
 * - Secondary invalidation: BlockEvent.BreakEvent / PlaceEvent invalidate any
 *   cached ray whose straight-line segment passes near the changed block.
 * - Fallback safety net: max cache age ~30 ticks regardless.
 *
 * The dose APPLICATION (who receives what, ARS, cancer) is the job of later
 * phases (Phase 4 / Phase 5). This class only answers the geometry question:
 * "given a source at X emitting strength S, how much reaches entity E through
 * the current blocks?"
 */
public final class RadiationOcclusion {

    private RadiationOcclusion() {}

    /** Minimum distance floor (blocks) to avoid division blowup at point-blank range. Mirrors ContaminationUtil.radiate()'s range*0.05 clamp spirit. */
    private static final double MIN_DISTANCE = 0.5D;
    /** Hard cap on cache age in ticks (fallback net). */
    private static final int MAX_CACHE_AGE = 30;

    /**
     * A registered discrete radiation source: a position in the world emitting a
     * continuous strength (in RAD/s-equivalent terms — the consuming system
     * decides tick scaling). Stored per-world.
     */
    public static final class RadiationSource {
        public final BlockPos pos;
        public final double strength;
        /** Eye-height offset of the actual emission point relative to the block's base. */
        public final double yOffset;

        public RadiationSource(BlockPos pos, double strength) {
            this(pos, strength, 0.5D);
        }

        public RadiationSource(BlockPos pos, double strength, double yOffset) {
            this.pos = pos;
            this.strength = strength;
            this.yOffset = yOffset;
        }
    }

    /** Per-world registry of active sources. Keyed by source position. */
    private static final Map<Integer, Map<BlockPos, RadiationSource>> SOURCES_BY_DIM = new HashMap<>();

    private static Map<BlockPos, RadiationSource> sourcesFor(World world) {
        return SOURCES_BY_DIM.computeIfAbsent(world.provider.getDimension(), k -> new HashMap<>());
    }

    // ---- Source registration API (consumed by Phase 3 tile entities) ----

    public static void registerSource(World world, RadiationSource source) {
        if (world == null || world.isRemote) return;
        sourcesFor(world).put(source.pos, source);
    }

    public static void deregisterSource(World world, BlockPos pos) {
        if (world == null || world.isRemote) return;
        Map<BlockPos, RadiationSource> map = SOURCES_BY_DIM.get(world.provider.getDimension());
        if (map != null) map.remove(pos);
    }

    public static void deregisterAll(World world) {
        if (world == null || world.isRemote) return;
        SOURCES_BY_DIM.remove(world.provider.getDimension());
    }

    // ---- Ray walk ----

    /**
     * Returns the occlusion-transmitted fraction (0..1) of radiation traveling
     * from the source emission point to the target point, computed by marching
     * the straight line through blocks and summing their HVL contributions.
     *
     * @param world        the world
     * @param sourcePos    source block position
     * @param sourceYOff   emission-point Y offset within the source block
     * @param target       target position (typically entity eye height)
     */
    public static double getTransmitted(World world, BlockPos sourcePos, double sourceYOff, Vec3 target) {
        double sx = sourcePos.getX() + 0.5D;
        double sy = sourcePos.getY() + sourceYOff;
        double sz = sourcePos.getZ() + 0.5D;

        Vec3 source = new Vec3(sx, sy, sz);
        Vec3 delta = target.subtract(source);
        double len = delta.length();
        if (len < 1e-6D) return 1.0D; // source and target essentially coincident

        Vec3 dir = delta.normalize();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        double opticalDepth = 0.0D;
        // March integer steps from just outside the source block up to (but not
        // including) the target's own block, mirroring ContaminationUtil.radiate()'s
        // `for(int i = 1; i < len; i++)` convention.
        int steps = (int) Math.floor(len);
        for (int i = 1; i < steps; i++) {
            int ix = (int) Math.floor(sx + dir.xCoord * i);
            int iy = (int) Math.floor(sy + dir.yCoord * i);
            int iz = (int) Math.floor(sz + dir.zCoord * i);
            pos.setPos(ix, iy, iz);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock().isAir(state, world, pos)) continue;
            opticalDepth += ShieldingRegistry.getHVLPerBlock(world, pos, state);
        }

        return Math.pow(0.5D, opticalDepth);
    }

    /**
     * Convenience: total effective RAD/s-equivalent dose an entity receives from
     * all registered sources in its world, after occlusion and inverse-square.
     * Returns 0 if no sources, or the entity is beyond the configured max range.
     */
    public static double getDoseForEntity(World world, EntityLivingBase entity) {
        if (world == null || world.isRemote) return 0.0D;
        Map<BlockPos, RadiationSource> sources = SOURCES_BY_DIM.get(world.provider.getDimension());
        if (sources == null || sources.isEmpty()) return 0.0D;

        double total = 0.0D;
        Vec3 target = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);

        for (RadiationSource src : sources.values()) {
            double dx = target.xCoord - (src.pos.getX() + 0.5D);
            double dy = target.yCoord - (src.pos.getY() + src.yOffset);
            double dz = target.zCoord - (src.pos.getZ() + 0.5D);
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > RadiationConfig.occlusionMaxRange) continue;

            double effDist = Math.max(dist, MIN_DISTANCE);
            double transmitted = getTransmitted(world, src.pos, src.yOffset, target);
            total += (src.strength / (effDist * effDist)) * transmitted;
        }
        return total;
    }

    /**
     * Geiger / on-demand single-source readout. Returns the occluded intensity at
     * an arbitrary queried position (not an entity). Computed on demand only.
     */
    public static double getIntensityAt(World world, RadiationSource src, Vec3 target) {
        double dx = target.xCoord - (src.pos.getX() + 0.5D);
        double dy = target.yCoord - (src.pos.getY() + src.yOffset);
        double dz = target.zCoord - (src.pos.getZ() + 0.5D);
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double effDist = Math.max(dist, MIN_DISTANCE);
        double transmitted = getTransmitted(world, src.pos, src.yOffset, target);
        return (src.strength / (effDist * effDist)) * transmitted;
    }

    // ---- Block-change invalidation ----
    // Caching is implemented at the consuming layer (per-entity dose caches in
    // Phase 4). This class exposes a coarse global invalidation hook so any
    // cached ray whose segment passes near a changed block can be dropped. The
    // consuming layer calls RadiationOcclusion.onBlockChanged(pos) from its
    // own BreakEvent/PlaceEvent handler, OR registers the listener below.

    /** Simple event listener registered in MainRegistry preInit. */
    public static final class BlockChangeInvalidator {
        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent event) {
            onBlockChanged(event.getWorld(), event.getPos());
        }

        @SubscribeEvent
        public void onPlace(BlockEvent.PlaceEvent event) {
            onBlockChanged(event.getWorld(), event.getPos());
        }
    }

    /**
     * Hook for consuming layers to clear their per-(source,target) caches when
     * the block at the given position changes. Over-invalidation is fine; the
     * recompute is cheap. The default implementation here is a no-op marker —
     * Phase 4's per-entity cache will register a callback via setCacheInvalidator.
     */
    private static volatile Runnable cacheInvalidator = null;

    public static void setCacheInvalidator(Runnable r) {
        cacheInvalidator = r;
    }

    public static void onBlockChanged(World world, BlockPos pos) {
        Runnable r = cacheInvalidator;
        if (r != null) r.run();
    }

    /** Per-tick maintenance (called from a TickEvent handler registered in MainRegistry). Clears stale source entries if needed. */
    public static void tick() {
        // Sources are deregistered explicitly by their tile entities; nothing to
        // sweep here yet. Reserved for future timeout/validation logic.
    }
}