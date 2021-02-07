package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.RegionConfig;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Context;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.support.WorldGuardUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class InfSpawnControler implements ISpawnControler {
    private final InfPlugin plugin;

    private Map<IMob, Player> mobPlayerMap = new LinkedHashMap<>();


    public InfSpawnControler(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean canSpawn(World world, Location location) {
        AtomicBoolean canSpawn = new AtomicBoolean(true);
        WorldConfig worldConfig;
        try {
            worldConfig = checkWorldConfigExistence(world);
        } catch (NoWorldConfigException ex) {
            return true;
        }
        if (MobManager.instance().getMobsInWorld(world).size() >= worldConfig.maxMobInWorld) {
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

    @Override
    public boolean canIMobAutoSpawn(World world) {
        return InfPlugin.plugin.config().worlds.get(world.getName()) != null;
    }

    @Override
    public boolean canVanillaAutoSpawn(World world) {
        try {
            WorldConfig worldConfig = checkWorldConfigExistence(world);
            return !worldConfig.disableNaturalSpawning;
        } catch (NoWorldConfigException e) {
            return true;
        }
    }

    @Override
    public int getMaxSpawnAmount(Player player) {
        World world = player.getWorld();
        WorldConfig worldConfig = checkWorldConfigExistence(world);
        return worldConfig.maxMobPerPlayer;
    }

    @Override
    public int getMaxSpawnAmount(World world) {
        WorldConfig worldConfig = checkWorldConfigExistence(world);
        return worldConfig.maxMobInWorld;
    }

    @Override
    public int getMaxSpawnDistance(World world) {
        WorldConfig worldConfig = checkWorldConfigExistence(world);
        return worldConfig.spawnRangeMax;
    }

    @Override
    public int getMinSpawnDistance(World world) {
        WorldConfig worldConfig = checkWorldConfigExistence(world);
        return worldConfig.spawnRangeMin;
    }

    private WorldConfig checkWorldConfigExistence(World world) {
        WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(world.getName());
        if (worldConfig == null) {
            throw new NoWorldConfigException();
        }
        return worldConfig;
    }

    @Override
    public void setVanillaAutoSpawn(World world, boolean flag) {
        WorldConfig worldConfig = checkWorldConfigExistence(world);
        worldConfig.disableNaturalSpawning = flag;
    }

    @Override
    public IMob spawnIMob(Player player, boolean force) {
        World world = player.getWorld();
        Location location = player.getLocation();
        int maxSpawnDistance = getMaxSpawnDistance(world);
        int minSpawnDistance = getMinSpawnDistance(world);
        Location spawnLocation;
        if (Utils.possibility(0.7)){
            spawnLocation = Utils.randomSpawnLocationInFront(location, minSpawnDistance, maxSpawnDistance);
        }else {
            spawnLocation = Utils.randomSpawnLocation(location, minSpawnDistance, maxSpawnDistance);
        }
        if (spawnLocation== null)return null;
        if (canSpawn(world,spawnLocation) || force) {
            if (!force && InfPlugin.wgEnabled){
                if (WorldGuardUtils.instance().isProtectedRegion(spawnLocation, player)) {
                    return null;
                }
            }
            centerSpawnLocation(spawnLocation);
            if (!lightValid(spawnLocation)){
                return null;
            }
            IMob iMob = MobManager.instance().natualSpawn(spawnLocation);
            if (iMob != null) {
                if (iMob.isDynamicHealth()) {
                    iMob.tweakHealth();
                }
                iMob.autoRetarget();
            }
            return iMob;
        } else return null;
    }

    private boolean lightValid(Location spawnLocation) {
        final Block block = spawnLocation.getBlock();
        final byte lightLevel = block.getLightLevel();
        final byte lightFromBlocks = block.getLightFromBlocks();
        final byte lightFromSky = block.getLightFromSky();
        final WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(block.getWorld().getName());
        if (worldConfig == null) {
            return false;
        }
        final int worldMinSkyLight = worldConfig.minSkyLight;
        final int worldMaxSkyLight = worldConfig.maxSkyLight;
        final int worldMinBlockLight = worldConfig.minBlockLight;
        final int worldMaxBlockLight = worldConfig.maxBlockLight;
        final int worldMinLight = worldConfig.minLight;
        final int worldMaxLight = worldConfig.maxLight;

        return isInRange(lightLevel, worldMinLight, worldMaxLight)
                && isInRange(lightFromBlocks, worldMinBlockLight, worldMaxBlockLight)
                && isInRange(lightFromSky, worldMinSkyLight, worldMaxSkyLight);
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
