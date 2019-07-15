package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Context;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class InfSpawnControler implements ISpawnControler {
    private final InfPlugin plugin;

    private Map<String, WorldConfig> worldConfigs;
    private Map<Player, List<IMob>> playerNearbyList = new LinkedHashMap<>();
    private Map<IMob, Player> mobPlayerMap = new LinkedHashMap<>();


    public InfSpawnControler(InfPlugin plugin) {
        this.plugin = plugin;
        worldConfigs = plugin.config().worlds;
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
        if (MobManager.instance().getMobsInWorld(world).size() > worldConfig.maxMobInWorld) {
            return false;
        }
        int maxSpawnDistance = getMaxSpawnDistance(world);
        Utils.getValidTargets(null, world.getNearbyEntities(location, maxSpawnDistance, maxSpawnDistance, maxSpawnDistance))
                .filter(entity -> entity instanceof Player)
                .map(entity -> ((Player) entity))
                .forEach(player -> {
                    boolean tempB = canSpawn.get();
                    canSpawn.set(tempB && canSpawnNearPlayer(player) && !isTooClose(player, location));
                });
        return canSpawn.get();
    }

    private boolean isTooClose(Player player, Location location) {
        return location.distance(player.getLocation()) < getMinSpawnDistance(player.getWorld());
    }

    @Override
    public boolean canSpawnNearPlayer(Player player) {
        int nearbyMobs = playerNearbyList.computeIfAbsent(player, player1 -> new ArrayList<>()).size();
        return nearbyMobs < getMaxSpawnAmount(player);
    }

    @Override
    public boolean canIMobAutoSpawn(World world) {
        return worldConfigs.get(world.getName()) != null;
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
        WorldConfig worldConfig = worldConfigs.get(world.getName());
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

        Location spawnLocation = Utils.randomSpawnLocation(location, minSpawnDistance, maxSpawnDistance);
        if (canSpawnNearPlayer(player) || force) {
            IMob iMob = MobManager.instance().natualSpawn(spawnLocation);
            registerMob(iMob);
            return iMob;
        } else return null;
    }

    private void registerMob(IMob iMob) {
        World world = iMob.getEntity().getWorld();
        int maxSpawnDistance = getMaxSpawnDistance(world);
        Utils.getValidTargets(iMob, world.getNearbyEntities(iMob.getEntity().getLocation(), maxSpawnDistance, maxSpawnDistance, maxSpawnDistance))
                .filter(entity -> entity instanceof Player)
                .map(entity -> ((Player) entity))
                .forEach(player -> {
                    List<IMob> iMobs = playerNearbyList.computeIfAbsent(player, player1 -> new ArrayList<>());
                    iMobs.add(iMob);
                    mobPlayerMap.put(iMob, null);
                });
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
    public void HandleSpawnEvent(CreatureSpawnEvent event) {
        World world = event.getLocation().getWorld();
        if (world == null) return;
        boolean isIMob = Context.instance().getBoolean(MobManager.MOB_SPAWN_CONTEXT, MobManager.IS_IMOB);
        if (isIMob) {
            if (!canSpawn(world, event.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)) {
            if (!canVanillaAutoSpawn(world)) return;
            event.setCancelled(true);
            return;
        }
    }

    @Override
    public void HandleMobDeath(EntityDeathEvent event) {

    }
}
