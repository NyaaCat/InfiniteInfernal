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

    private boolean isTooClose(Player player, Location location) {
        return location.distance(player.getLocation()) < getMinSpawnDistance(player.getWorld());
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
        int nearbyMobs = (int) MobManager.instance().getMobsNearPlayer(player)
                .stream().filter(iMob -> {
                    if (regionsForLocation.isEmpty())return true;
                    Location location1 = iMob.getEntity().getLocation();
                    return regionsForLocation.stream().anyMatch(regionConfig -> regionConfig.region.contains(location1));
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
        Function<MobConfig, Location> locationSupplier = (candidate) ->
                findLocationByConfig(player, candidate, center, force, finalAllowedRegions);
        Location location = locationSupplier.apply(mobConfig);
        int retryTimes = 30;
        for (int i = 0; i < retryTimes; i++) {
            if (location != null){
                break;
            }
            location = locationSupplier.apply(mobConfig);
        }
        if (location == null){
            return null;
        }
        return mobSupplier.apply(location);
    }

    private Location findLocationByConfig(Player player, MobConfig mobConfig, Location center, boolean force, List<RegionConfig> allowedRegions) {
        World world = center.getWorld();
        Location spawnLocation = null;
        final EntityType type = mobConfig.type;
        if (MobManager.FluidLocationWrapper.isSkyMob(type)){
            spawnLocation = findSkyLocation(world, center);
        }else if (MobManager.FluidLocationWrapper.isWaterMob(type)){
            spawnLocation = findWaterLocation(world, center);
        }else {
            spawnLocation = findFloorLocation(world, center);
        }

        if (spawnLocation == null)return null;
        if (recheckLocation(spawnLocation, mobConfig, force, player, allowedRegions)){
            centerSpawnLocation(spawnLocation);
            return spawnLocation;
        }else return null;
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

    private boolean recheckLocation(Location location, MobConfig mobConfig, boolean force, Player player, List<RegionConfig> allowedRegions) {
        if (location == null || mobConfig == null || location.getWorld() == null) {
            return false;
        }
        // Block spawning within regions that have empty mobs list
        if (!force && isInEmptyMobsRegion(location)) {
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
