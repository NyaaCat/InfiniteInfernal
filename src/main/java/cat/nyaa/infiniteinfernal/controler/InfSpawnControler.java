package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.configs.RegionConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Context;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import cat.nyaa.infiniteinfernal.utils.support.WorldGuardUtils;
import cat.nyaa.nyaacore.Pair;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

public class InfSpawnControler implements ISpawnControler {
    private final InfPlugin plugin;

    private Map<IMob, Player> mobPlayerMap = new LinkedHashMap<>();


    public InfSpawnControler(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canSpawn(World world, Location location) {
        final Config config = InfPlugin.plugin.config();
        AtomicBoolean canSpawn = new AtomicBoolean(true);
        if (MobManager.instance().getMobsInWorld(world).size() >= config.getMaxMobInWorld(world)) {
            return false;
        }
        if (isBossNearby(location)) {
            return false;
        }
        int maxSpawnDistance = getMaxSpawnDistance(world);
        world.getNearbyEntities(location, maxSpawnDistance*1.5, maxSpawnDistance*1.5, maxSpawnDistance*1.5).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> ((Player) entity))
                .filter(player -> !player.getGameMode().equals(GameMode.SPECTATOR))
                .forEach(player -> {
                    boolean tempB = canSpawn.get();
                    if (!tempB) return;
                    canSpawn.set(canSpawnNearPlayer(player, location) && !isTooClose(player, location));
                });
        return canSpawn.get();
    }

    @Override
    public boolean canSpawnNearPlayer(Player player, Location location) {
        if(player.getGameMode().equals(GameMode.SPECTATOR)){
            return false;
        }
        Location playerLocation = player.getLocation();
        List<RegionConfig> regionsForLocation = InfPlugin.plugin.config().getRegionsForLocation(playerLocation);
        if (!regionsForLocation.isEmpty() && regionsForLocation.stream().noneMatch(regionConfig -> regionConfig.region.contains(location))){
            return false;
        }

        // Calculate nearby mobs directly for accuracy instead of relying on cached list
        // The cached list (getMobsNearPlayer) is updated asynchronously and may be stale
        // during a spawn cycle where multiple mobs spawn in quick succession
        World world = player.getWorld();
        int nearbyDistance = getMaxSpawnDistance(world);
        List<IMob> allMobsInWorld = MobManager.instance().getMobsInWorld(world);

        int nearbyMobs = (int) allMobsInWorld.stream()
                .filter(iMob -> {
                    LivingEntity entity = iMob.getEntity();
                    if (entity == null || entity.isDead()) return false;
                    if (!entity.getWorld().equals(world)) return false;

                    // Check if mob is within spawn range of player
                    double distance = entity.getLocation().distance(playerLocation);
                    if (distance > nearbyDistance * 1.5) return false;

                    // Apply region filter if player is in a region
                    if (regionsForLocation.isEmpty()) return true;
                    Location mobLocation = entity.getLocation();
                    return regionsForLocation.stream().anyMatch(regionConfig -> regionConfig.region.contains(mobLocation));
                }).count();

        return nearbyMobs < getMaxSpawnAmount(player);
    }

    /**
     * Checks if a location is within a region that has an empty mobs list.
     * Empty mobs list means spawning is blocked in that region.
     */
    private boolean isInEmptyMobsRegion(Location location) {
        List<RegionConfig> regions = InfPlugin.plugin.config().getRegionsForLocation(location);
        if (regions.isEmpty()) {
            return false;
        }
        return regions.stream().anyMatch(regionConfig -> regionConfig.mobs.isEmpty());
    }

    @Override
    public boolean canIMobAutoSpawn(World world) {
        return InfPlugin.plugin.config().isEnabledInWorld(world);
    }

    @Override
    public boolean canVanillaAutoSpawn(World world) {
        return !InfPlugin.plugin.config().isAutoSpawnDisabledInWorld(world);
    }

    @Override
    public int getMaxSpawnAmount(Player player) {
        final Config config = InfPlugin.plugin.config();
        return config.getMaxMobPerPlayer(player.getWorld());
    }

    @Override
    public int getMaxSpawnAmount(World world) {
        final Config config = InfPlugin.plugin.config();
        return config.getMaxMobInWorld(world);
    }

    @Override
    public int getMaxSpawnDistance(World world) {
        final Config config = InfPlugin.plugin.config();
        return config.getSpawnRangeMax(world);
    }

    @Override
    public int getMinSpawnDistance(World world) {
        final Config config = InfPlugin.plugin.config();
        return config.getSpawnRangeMin(world);
    }

    @Override
    public void setVanillaAutoSpawn(World world, boolean flag) {
        final Config config = InfPlugin.plugin.config();
        config.setAutoSpawnDisabledInWorld(world, !flag);
        config.save();
    }

    @Override
    public IMob spawnIMob(Player player, boolean force) {
        MobManager mobManager = MobManager.instance();
        Location center = player.getLocation();
        Config config = InfPlugin.plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(center);

        Function<Location, IMob> mobSupplier = null;
        MobConfig mobConfig = null;
        List<RegionConfig> allowedRegions = null;

        if (!canSpawnNearPlayer(player, center) && !force) {
            return null;
        }

        if (!regions.isEmpty()) {
            // Check if player is in a region with empty mobs list - block all spawning
            if (isInEmptyMobsRegion(center)) {
                return null;
            }
            WeightedPair<MobConfig, Integer> pair = mobManager.selectConfigInRegion(regions, center);
            if (pair != null && pair.getKey() != null) {
                mobConfig = pair.getKey();
                final Integer level = pair.getValue();
                MobConfig finalMobConfig = mobConfig;
                mobSupplier = (location) -> mobManager.spawnMobByConfig(finalMobConfig, location, level);
                allowedRegions = regions;
            } else {
                // Player is in a region but no mobs are configured - don't fall back to natural spawning
                return null;
            }
        }

        if (mobSupplier == null){
            Pair<MobConfig, Integer> pair = mobManager.selectNatualMobConfig(center);
            if (pair == null || pair.getKey() == null){
                return null;
            }
            mobConfig = pair.getKey();
            final Integer level = pair.getValue();
            MobConfig finalMobConfig = mobConfig;
            mobSupplier = (location) -> mobManager.spawnMobByConfig(finalMobConfig, location, level);
        }

        List<RegionConfig> finalAllowedRegions = allowedRegions;
        // findLocationByConfig now handles multiple attempts internally with LOS prioritization
        Location location = findLocationByConfig(player, mobConfig, center, force, finalAllowedRegions);

        // Additional retries if first attempt fails
        for (int i = 0; i < 3 && location == null; i++) {
            location = findLocationByConfig(player, mobConfig, center, force, finalAllowedRegions);
        }

        if (location == null) {
            return null;
        }
        return mobSupplier.apply(location);
    }

    private Location findLocationByConfig(Player player, MobConfig mobConfig, Location center, boolean force, List<RegionConfig> allowedRegions) {
        World world = center.getWorld();
        final EntityType type = mobConfig.type;

        // Collect multiple candidate locations and prefer those with LOS
        List<Location> candidates = new ArrayList<>();
        Location losCandidate = null;
        Location anyCandidate = null;

        // Try to find locations, prioritizing LOS
        int maxAttempts = 15;
        for (int attempt = 0; attempt < maxAttempts && losCandidate == null; attempt++) {
            Location spawnLocation = findRawLocation(world, center, type, allowedRegions);

            if (spawnLocation == null) continue;
            if (!recheckLocation(spawnLocation, mobConfig, force, player, allowedRegions)) continue;

            centerSpawnLocation(spawnLocation);

            if (anyCandidate == null) {
                anyCandidate = spawnLocation;
            }

            if (hasLineOfSight(player, spawnLocation)) {
                losCandidate = spawnLocation;
            } else {
                candidates.add(spawnLocation);
            }
        }

        // Prefer LOS location (80% of successful spawns should have LOS)
        if (losCandidate != null) {
            return losCandidate;
        }

        // Allow non-LOS spawn occasionally for variety/ambush (20% chance if we have candidates)
        if (anyCandidate != null && Utils.possibility(0.2)) {
            return anyCandidate;
        }

        // Try a few more times specifically for LOS
        for (int attempt = 0; attempt < 5; attempt++) {
            Location spawnLocation = findRawLocation(world, center, type, allowedRegions);
            if (spawnLocation == null) continue;
            if (!recheckLocation(spawnLocation, mobConfig, force, player, allowedRegions)) continue;
            centerSpawnLocation(spawnLocation);
            if (hasLineOfSight(player, spawnLocation)) {
                return spawnLocation;
            }
        }

        // If still no LOS location, use any valid candidate
        return anyCandidate;
    }

    /**
     * Finds a raw spawn location without LOS checking.
     */
    private Location findRawLocation(World world, Location center, EntityType type, List<RegionConfig> allowedRegions) {
        if (allowedRegions != null && !allowedRegions.isEmpty()) {
            if (MobManager.FluidLocationWrapper.isSkyMob(type)) {
                return findSkyLocationInRegion(world, center, allowedRegions);
            } else if (MobManager.FluidLocationWrapper.isWaterMob(type)) {
                return findWaterLocationInRegion(world, center, allowedRegions);
            } else {
                return findFloorLocationInRegion(world, center, allowedRegions);
            }
        } else {
            if (MobManager.FluidLocationWrapper.isSkyMob(type)) {
                return findSkyLocation(world, center);
            } else if (MobManager.FluidLocationWrapper.isWaterMob(type)) {
                return findWaterLocation(world, center);
            } else {
                return findFloorLocation(world, center);
            }
        }
    }

    /**
     * Checks if there is a clear line of sight from the player to the spawn location.
     * Uses raytrace to detect solid blocks between the two points.
     */
    private boolean hasLineOfSight(Player player, Location spawnLocation) {
        if (player == null || spawnLocation == null) {
            return false;
        }

        Location eyeLocation = player.getEyeLocation();
        World world = eyeLocation.getWorld();
        if (world == null || !world.equals(spawnLocation.getWorld())) {
            return false;
        }

        // Adjust spawn location to approximate mob eye level (1.5 blocks up from feet)
        Location targetLocation = spawnLocation.clone().add(0, 1.5, 0);

        Vector direction = targetLocation.toVector().subtract(eyeLocation.toVector());
        double distance = direction.length();

        if (distance < 1) {
            return true; // Too close, consider LOS
        }

        direction.normalize();

        // Raycast from player eye to spawn location
        RayTraceResult result = world.rayTraceBlocks(
                eyeLocation,
                direction,
                distance,
                FluidCollisionMode.NEVER,
                true
        );

        // If no hit, there's clear LOS
        if (result == null) {
            return true;
        }

        // Check if the hit block is very close to the target (within 2 blocks)
        Location hitLocation = result.getHitPosition().toLocation(world);
        return hitLocation.distance(targetLocation) < 2.0;
    }

    private Location findSkyLocation(World world, Location center) {
        if (world == null || center == null) {
            return null;
        }
        int maxSpawnDistance = getMaxSpawnDistance(world);
        int minSpawnDistance = getMinSpawnDistance(world);
        for (int i = 0; i < 20; i++) {
            Location candidate = Utils.randomSpawnLocation(center, minSpawnDistance, maxSpawnDistance, location -> true);
            if (candidate == null) {
                continue;
            }
            int x = candidate.getBlockX();
            int z = candidate.getBlockZ();
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int minY = world.getMinHeight() + 1;
            int maxY = world.getMaxHeight() - 2;
            if (maxY <= minY) {
                continue;
            }
            int y = (int) Math.round(Utils.random(minY, maxY));
            Location spawn = new Location(world, x + 0.5, y, z + 0.5);
            if (spawn.getBlock().getType().isAir() && spawn.getBlock().getRelative(BlockFace.UP).getType().isAir()) {
                return spawn;
            }
        }
        return null;
    }

    private Location findWaterLocation(World world, Location center) {
        if (world == null || center == null) {
            return null;
        }
        int maxSpawnDistance = getMaxSpawnDistance(world);
        int minSpawnDistance = getMinSpawnDistance(world);
        for (int i = 0; i < 20; i++) {
            Location candidate = Utils.randomSpawnLocation(center, minSpawnDistance, maxSpawnDistance, location -> true);
            if (candidate == null) {
                continue;
            }
            int x = candidate.getBlockX();
            int z = candidate.getBlockZ();
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int topY = world.getHighestBlockYAt(x, z);
            int minY = world.getMinHeight();
            for (int y = topY; y >= minY; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    Block above = block.getRelative(BlockFace.UP);
                    if (above.getType() == Material.WATER || above.getType().isAir()) {
                        return block.getLocation().add(0.5, 0, 0.5);
                    }
                    continue;
                }
                if (block.getType().isSolid()) {
                    break;
                }
            }
        }
        return null;
    }

    private Location findLocation(World world, Location center, Predicate<Material> air) {
        int maxSpawnDistance = getMaxSpawnDistance(world);
        int minSpawnDistance = getMinSpawnDistance(world);
        Location spawnLocation;
        if (Utils.possibility(0.7)){
            spawnLocation = Utils.randomSpawnLocationInFront(center, minSpawnDistance, maxSpawnDistance, (location -> air.test(location.getBlock().getType())));
        }else {
            spawnLocation = Utils.randomSpawnLocation(center, minSpawnDistance, maxSpawnDistance, (location -> air.test(location.getBlock().getType())));
        }
        return spawnLocation;
    }

    private Location findFloorLocation(World world, Location location) {
        int maxSpawnDistance = getMaxSpawnDistance(world);
        int minSpawnDistance = getMinSpawnDistance(world);
        Location spawnLocation;
        if (Utils.possibility(0.7)){
            spawnLocation = Utils.randomFloorSpawnLocationInFront(location, minSpawnDistance, maxSpawnDistance);
        }else {
            spawnLocation = Utils.randomFloorSpawnLocation(location, minSpawnDistance, maxSpawnDistance);
        }
        return spawnLocation;
    }

    /**
     * Finds a floor spawn location constrained to within the allowed regions.
     * This prevents spawn attempts from failing due to locations falling outside region bounds.
     */
    private Location findFloorLocationInRegion(World world, Location center, List<RegionConfig> regions) {
        if (world == null || center == null || regions == null || regions.isEmpty()) {
            return findFloorLocation(world, center);
        }

        int minSpawnDistance = getMinSpawnDistance(world);
        int maxSpawnDistance = getMaxSpawnDistance(world);

        for (int i = 0; i < 20; i++) {
            Location candidate;
            if (Utils.possibility(0.7)) {
                candidate = Utils.randomFloorSpawnLocationInFront(center, minSpawnDistance, maxSpawnDistance);
            } else {
                candidate = Utils.randomFloorSpawnLocation(center, minSpawnDistance, maxSpawnDistance);
            }

            if (candidate != null && isInAnyRegion(candidate, regions) && !isTooClose(null, candidate)) {
                return candidate;
            }
        }

        // Fallback: try spawning directly within region bounds
        for (RegionConfig region : regions) {
            Location candidate = randomLocationInRegion(world, region, center, minSpawnDistance);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Finds a sky spawn location constrained to within the allowed regions.
     */
    private Location findSkyLocationInRegion(World world, Location center, List<RegionConfig> regions) {
        if (world == null || center == null || regions == null || regions.isEmpty()) {
            return findSkyLocation(world, center);
        }

        int minSpawnDistance = getMinSpawnDistance(world);
        int maxSpawnDistance = getMaxSpawnDistance(world);

        for (int i = 0; i < 20; i++) {
            Location candidate = Utils.randomSpawnLocation(center, minSpawnDistance, maxSpawnDistance, location -> true);
            if (candidate == null) {
                continue;
            }
            if (!isInAnyRegion(candidate, regions)) {
                continue;
            }
            int x = candidate.getBlockX();
            int z = candidate.getBlockZ();
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            // For regions, use region Y bounds instead of world bounds
            int minY = regions.stream().mapToInt(r -> r.region.yMin).min().orElse(world.getMinHeight() + 1);
            int maxY = regions.stream().mapToInt(r -> r.region.yMax).max().orElse(world.getMaxHeight() - 2);
            if (maxY <= minY) {
                continue;
            }
            int y = (int) Math.round(Utils.random(minY, maxY));
            Location spawn = new Location(world, x + 0.5, y, z + 0.5);
            if (spawn.getBlock().getType().isAir() && spawn.getBlock().getRelative(org.bukkit.block.BlockFace.UP).getType().isAir()) {
                if (!isTooClose(null, spawn) && isInAnyRegion(spawn, regions)) {
                    return spawn;
                }
            }
        }

        // Fallback: try spawning directly within region bounds
        for (RegionConfig regionConfig : regions) {
            Location candidate = randomSkyLocationInRegion(world, regionConfig, center, minSpawnDistance);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Generates a random sky spawn location within a region's bounds.
     */
    private Location randomSkyLocationInRegion(World world, RegionConfig regionConfig, Location playerLoc, int minDistance) {
        if (regionConfig == null || regionConfig.region == null || world == null) {
            return null;
        }
        RegionConfig.Region region = regionConfig.region;
        for (int attempt = 0; attempt < 15; attempt++) {
            int x = region.xMin + (int) (Utils.random() * (region.xMax - region.xMin));
            int z = region.zMin + (int) (Utils.random() * (region.zMax - region.zMin));
            int y = region.yMin + (int) (Utils.random() * (region.yMax - region.yMin));

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            Location spawn = new Location(world, x + 0.5, y, z + 0.5);

            // Check minimum distance from player
            if (playerLoc != null && spawn.distance(playerLoc) < minDistance) {
                continue;
            }

            // Sky mobs just need air space
            if (spawn.getBlock().getType().isAir() && spawn.getBlock().getRelative(org.bukkit.block.BlockFace.UP).getType().isAir()) {
                if (region.contains(spawn) && !isTooClose(null, spawn)) {
                    return spawn;
                }
            }
        }
        return null;
    }

    /**
     * Finds a water spawn location constrained to within the allowed regions.
     */
    private Location findWaterLocationInRegion(World world, Location center, List<RegionConfig> regions) {
        if (world == null || center == null || regions == null || regions.isEmpty()) {
            return findWaterLocation(world, center);
        }

        int minSpawnDistance = getMinSpawnDistance(world);
        int maxSpawnDistance = getMaxSpawnDistance(world);

        for (int i = 0; i < 20; i++) {
            Location candidate = Utils.randomSpawnLocation(center, minSpawnDistance, maxSpawnDistance, location -> true);
            if (candidate == null) {
                continue;
            }
            if (!isInAnyRegion(candidate, regions)) {
                continue;
            }
            int x = candidate.getBlockX();
            int z = candidate.getBlockZ();
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int topY = world.getHighestBlockYAt(x, z);
            int maxY = regions.stream().mapToInt(r -> r.region.yMax).max().orElse(world.getMaxHeight());
            int minY = regions.stream().mapToInt(r -> r.region.yMin).min().orElse(world.getMinHeight());
            int searchStartY = Math.min(topY, maxY);
            for (int y = searchStartY; y >= minY; y--) {
                org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    org.bukkit.block.Block above = block.getRelative(org.bukkit.block.BlockFace.UP);
                    if (above.getType() == Material.WATER || above.getType().isAir()) {
                        Location spawn = block.getLocation().add(0.5, 0, 0.5);
                        if (!isTooClose(null, spawn) && isInAnyRegion(spawn, regions)) {
                            return spawn;
                        }
                    }
                    continue;
                }
                if (block.getType().isSolid()) {
                    break;
                }
            }
        }

        // Fallback: try spawning directly within region bounds
        for (RegionConfig regionConfig : regions) {
            Location candidate = randomWaterLocationInRegion(world, regionConfig, center, minSpawnDistance);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Generates a random water spawn location within a region's bounds.
     */
    private Location randomWaterLocationInRegion(World world, RegionConfig regionConfig, Location playerLoc, int minDistance) {
        if (regionConfig == null || regionConfig.region == null || world == null) {
            return null;
        }
        RegionConfig.Region region = regionConfig.region;
        for (int attempt = 0; attempt < 15; attempt++) {
            int x = region.xMin + (int) (Utils.random() * (region.xMax - region.xMin));
            int z = region.zMin + (int) (Utils.random() * (region.zMax - region.zMin));

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            // Search for water within region Y bounds
            for (int y = region.yMax; y >= region.yMin; y--) {
                org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    org.bukkit.block.Block above = block.getRelative(org.bukkit.block.BlockFace.UP);
                    if (above.getType() == Material.WATER || above.getType().isAir()) {
                        Location spawn = block.getLocation().add(0.5, 0, 0.5);

                        // Check minimum distance from player
                        if (playerLoc != null && spawn.distance(playerLoc) < minDistance) {
                            continue;
                        }

                        if (region.contains(spawn) && !isTooClose(null, spawn)) {
                            return spawn;
                        }
                    }
                    continue;
                }
                if (block.getType().isSolid()) {
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Checks if a location is within any of the given regions.
     */
    private boolean isInAnyRegion(Location location, List<RegionConfig> regions) {
        if (location == null || regions == null) {
            return false;
        }
        return regions.stream().anyMatch(r -> r.region != null && r.region.contains(location));
    }

    /**
     * Generates a random spawn location within a region's bounds.
     */
    private Location randomLocationInRegion(World world, RegionConfig regionConfig, Location playerLoc, int minDistance) {
        if (regionConfig == null || regionConfig.region == null || world == null) {
            return null;
        }
        RegionConfig.Region region = regionConfig.region;
        for (int attempt = 0; attempt < 15; attempt++) {
            int x = region.xMin + (int) (Utils.random() * (region.xMax - region.xMin));
            int z = region.zMin + (int) (Utils.random() * (region.zMax - region.zMin));

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            // Search for valid Y within region bounds
            for (int y = region.yMax; y >= region.yMin; y--) {
                Location candidate = new Location(world, x + 0.5, y, z + 0.5);

                // Check minimum distance from player
                if (playerLoc != null && candidate.distance(playerLoc) < minDistance) {
                    continue;
                }

                Location validLoc = Utils.findValidSpawnLocationInY(candidate);
                if (validLoc != null && region.contains(validLoc)) {
                    return validLoc;
                }
            }
        }
        return null;
    }

    private boolean isTooClose(Player player, Location location) {
        if (player == null) {
            // When player is null, check against all nearby players
            World world = location.getWorld();
            if (world == null) return false;
            int minDistance = getMinSpawnDistance(world);
            return world.getPlayers().stream()
                    .filter(p -> !p.getGameMode().equals(GameMode.SPECTATOR))
                    .anyMatch(p -> location.distance(p.getLocation()) < minDistance);
        }
        return location.distance(player.getLocation()) < getMinSpawnDistance(player.getWorld());
    }

    private boolean recheckLocation(Location location, MobConfig mobConfig, boolean force, Player player, List<RegionConfig> allowedRegions) {
        if (location == null || mobConfig == null || location.getWorld() == null) {
            return false;
        }
        // Block spawning within regions that have empty mobs list
        if (!force && isInEmptyMobsRegion(location)) {
            return false;
        }
        if (!force && isBossNearby(location)) {
            return false;
        }
        if (!force) {
            if (allowedRegions != null && !allowedRegions.isEmpty()) {
                boolean inAllowedRegion = allowedRegions.stream()
                        .anyMatch(regionConfig -> regionConfig.region != null && regionConfig.region.contains(location));
                if (!inAllowedRegion) {
                    return false;
                }
            }
        }
        World world = location.getWorld();
        if (canSpawn(world,location) || force) {
            if (!isValidWorld(mobConfig, world)) {
                return false;
            }
            boolean ignoreBiome = allowedRegions != null && !allowedRegions.isEmpty()
                    && isRegionDefinedMob(allowedRegions, mobConfig);
            if (!ignoreBiome) {
                Biome biome = location.getBlock().getBiome();
                if (!isValidBiome(mobConfig, biome)) {
                    return false;
                }
            }
            if (!force && InfPlugin.wgEnabled){
                if (WorldGuardUtils.instance().isProtectedRegion(location, player)) {
                    return false;
                }
            }
            centerSpawnLocation(location);
            if (!lightValid(location)){
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isValidWorld(MobConfig mobConfig, World world) {
        List<String> worlds = mobConfig.spawn.worlds;
        return worlds == null || worlds.isEmpty() || worlds.contains(world.getName());
    }

    private boolean isValidBiome(MobConfig mobConfig, Biome biome) {
        List<String> biomes = mobConfig.spawn.biomes;
        return MobManager.isBiomeMatch(biomes, biome.getKey().getKey());
    }

    private boolean isRegionDefinedMob(List<RegionConfig> regions, MobConfig mobConfig) {
        if (regions == null || regions.isEmpty() || mobConfig == null) {
            return false;
        }
        String name = mobConfig.getName();
        for (RegionConfig regionConfig : regions) {
            for (String entry : regionConfig.mobs) {
                if (entry == null || entry.isEmpty()) {
                    continue;
                }
                int colon = entry.indexOf(':');
                String mobId = colon >= 0 ? entry.substring(0, colon) : entry;
                if (name.equals(mobId.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean lightValid(Location spawnLocation) {
        final Config config = InfPlugin.plugin.config();
        final World world = spawnLocation.getWorld();

        final Block block = spawnLocation.getBlock();
        final byte lightLevel = block.getLightLevel();
        final byte lightFromBlocks = block.getLightFromBlocks();
        final byte lightFromSky = block.getLightFromSky();

        final int worldMinSkyLight = config.getSpawnMinSkyLight(world);
        final int worldMaxSkyLight = config.getSpawnMaxSkyLight(world);
        final int worldMinBlockLight = config.getSpawnMinBlockLight(world);
        final int worldMaxBlockLight = config.getSpawnMaxBlockLight(world);
        final int worldMinLight = config.getSpawnMinLight(world);
        final int worldMaxLight = config.getSpawnMaxLight(world);

        return isInRange(lightLevel, worldMinLight, worldMaxLight)
                || isInRange(lightFromBlocks, worldMinBlockLight, worldMaxBlockLight)
                || isInRange(lightFromSky, worldMinSkyLight, worldMaxSkyLight);
    }

    private boolean isInRange(byte lightLevel, int worldMinLight, int worldMaxLight) {
        return lightLevel >= worldMinLight && lightLevel <= worldMaxLight;
    }

    private void centerSpawnLocation(Location spawnLocation) {
        int blockX = spawnLocation.getBlockX();
        int blockZ = spawnLocation.getBlockZ();
        spawnLocation.setX(blockX + 0.5);
        spawnLocation.setZ(blockZ + 0.5);
    }

    private boolean isBossNearby(Location location) {
        if (location == null) {
            return false;
        }
        Config config = InfPlugin.plugin.config();
        String tag = config.bossSpawnBlockTag;
        int range = config.bossSpawnBlockRange;
        if (tag == null || tag.isEmpty() || range <= 0) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        double rangeSq = (double) range * range;
        List<IMob> mobs = MobManager.instance().getMobsInWorld(world);
        if (mobs.isEmpty()) {
            return false;
        }
        for (IMob iMob : mobs) {
            LivingEntity entity = iMob.getEntity();
            if (entity == null || entity.isDead()) {
                continue;
            }
            if (!entity.getScoreboardTags().contains(tag)) {
                continue;
            }
            if (entity.getLocation().distanceSquared(location) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IMob spawnIMob(Location location, boolean force) {
        World world = location.getWorld();
        if (!force) {
            if (!canSpawn(world, location)) return null;
        }
        IMob iMob = null;
        if (world != null) {
            iMob = MobManager.instance().natualSpawn(location);
        }
        return iMob;
    }

    @Override
    public LivingEntity spawnVanilla(Player player, boolean force) {
        World world = player.getWorld();
        Location location = player.getLocation();
        Biome biome = world.getBiome(location.getBlockX(), location.getBlockZ());
        //f@@k it's hard
        throw new UnsupportedOperationException("method not implemented");
    }

    @Override
    public void handleSpawnEvent(CreatureSpawnEvent event) {
        World world = event.getLocation().getWorld();
        if (world == null) return;
        Boolean isIMob = Context.instance().getBoolean(MobManager.MOB_SPAWN_CONTEXT, MobManager.IS_IMOB);
        if (isIMob != null && isIMob) {
            if (!canSpawn(world, event.getLocation())) {
//                event.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void handleMobDeath(EntityDeathEvent event) {
        IMob iMob = MobManager.instance().toIMob(event.getEntity());
        if (iMob == null) return;
        MobManager.instance().removeMob(iMob, true);
        mobPlayerMap.remove(iMob);
    }
}
