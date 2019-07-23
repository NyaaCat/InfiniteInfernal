package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.DirConfigs;
import cat.nyaa.infiniteinfernal.configs.LevelConfig;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.configs.RegionConfig;
import cat.nyaa.infiniteinfernal.utils.Context;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MobManager {
    private static MobManager instance;
    public static final UUID MOB_SPAWN_CONTEXT = UUID.randomUUID();
    public static final String IS_IMOB = "isIMob";

    Map<UUID, IMob> uuidMap = new LinkedHashMap<>();
    Map<World, List<IMob>> worldMobMap = new LinkedHashMap<>();
    Map<Player, List<IMob>> playerNearbyList = new LinkedHashMap<>();
    Map<IMob, List<Player>> mobNearbyList = new LinkedHashMap<>();

    Map<String, MobConfig> nameCfgMap = new LinkedHashMap<>();
    Map<String, MobConfig> idCfgMap = new LinkedHashMap<>();
    Map<Integer, List<MobConfig>> natualSpawnLists = new LinkedHashMap<>();

    private MobManager() {
        this.load();
    }

    public void load() {
        initialize();
        DirConfigs<MobConfig> mobConfigs = InfPlugin.plugin.config().mobConfigs;
        buildNatualSpawnList(mobConfigs);
        buildCfgMaps(mobConfigs);
    }

    private void initialize() {
        if ((!worldMobMap.values().isEmpty())) {
            worldMobMap.values().forEach(list -> {
                if (!list.isEmpty()) {
                    LinkedList<IMob> iMobs = new LinkedList<>(list);
                    IMob poll;
                    while ((poll = iMobs.poll()) != null) {
                        removeMob(poll);
                    }
                }
            });
        }
        worldMobMap.clear();
        nameCfgMap.clear();
        idCfgMap.clear();
        natualSpawnLists.clear();
    }

    private void buildCfgMaps(DirConfigs<MobConfig> mobConfigs) {
        mobConfigs.values().stream().parallel()
                .forEach(config -> {
                    nameCfgMap.put(config.name, config);
                    idCfgMap.put(config.id, config);
                });
    }

    private void buildNatualSpawnList(DirConfigs<MobConfig> mobConfigs) {
        mobConfigs.values().stream().parallel()
                .filter(config -> config.spawn.autoSpawn)
                .forEach(config -> {
                    List<Integer> validLevels = MobConfig.parseLevels(config.spawn.levels);
                    if ((!validLevels.isEmpty())) {
                        validLevels.forEach(level -> {
                            List<MobConfig> natualSpawnList = this.natualSpawnLists.computeIfAbsent(level, integer -> new ArrayList<>());
                            natualSpawnList.add(config);
                        });
                    }
                });
    }

    public static MobManager instance() {
        if (instance == null) {
            synchronized (MobManager.class) {
                if (instance == null) {
                    instance = new MobManager();
                }
            }
        }
        return instance;
    }

    public static void disable() {
        instance = null;
    }

    public IMob spawnMobByConfig(MobConfig config, Location location, Integer level) {
        if (config == null) return null;
        EntityType entityType = config.type;
        World world = location.getWorld();
        if (world != null) {
            Class<? extends Entity> entityClass = entityType.getEntityClass();
            if (entityClass != null && LivingEntity.class.isAssignableFrom(entityClass)) {
                if (level == null) {
                    List<String> levelStrs = config.spawn.levels;
                    List<Integer> levels = MobConfig.parseLevels(levelStrs);
                    level = Utils.randomPick(levels);
                }
                CustomMob customMob = new CustomMob(config, level);
                Context.instance().put(MOB_SPAWN_CONTEXT, IS_IMOB, true);
                LivingEntity spawn = (LivingEntity) world.spawn(location, entityClass);
                Context.instance().removeTemp(MOB_SPAWN_CONTEXT, IS_IMOB);
                customMob.makeInfernal(spawn);
                registerMob(customMob);
                return customMob;
            }
        }
        return null;
    }

    public Collection<IMob> getMobs() {
        return uuidMap.values();
    }

    public IMob spawnMobByName(String name, Location location, Integer level) {
        MobConfig mobConfig = nameCfgMap.get(name);
        return spawnMobByConfig(mobConfig, location, level);
    }

    public IMob spawnMobById(String mobId, Location location, Integer level) {
        MobConfig iMob = idCfgMap.get(mobId);
        if (iMob == null) {
            throw new IllegalArgumentException();
        }
        return spawnMobByConfig(iMob, location, level);
    }

    public IMob natualSpawn(Location location) {
        Config config = InfPlugin.plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(location);
        List<WeightedPair<MobConfig, Integer>> spawnConfs = new ArrayList<>();
        if (!regions.isEmpty()) {
            regions.forEach(regionConfig -> {
                if (regionConfig.mobs.isEmpty()) {
                    return;
                }
                regionConfig.mobs.forEach(mobs -> {
                    try {
                        String[] split = mobs.split(":");
                        String mobId = split[0];
                        int mobWeight = Integer.parseInt(split[1]);
                        MobConfig mobConfig = idCfgMap.get(mobId);
                        if (mobConfig == null) {
                            Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.spawn_no_id", mobs));
                            return;
                        }
                        Biome biome = location.getBlock().getBiome();
                        World world = location.getWorld();
                        if (mobConfig.spawn.biomes.contains(biome.name()) && world != null && mobConfig.spawn.worlds.contains(world.getName())) {
                            spawnConfs.add(new WeightedPair<>(mobConfig, 0, mobWeight));
                        }
                    } catch (NumberFormatException e) {
                        Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.num_format", mobs));
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.bad_config", mobs));
                    }
                });
            });
        }
        if (!spawnConfs.isEmpty()) {
            WeightedPair<MobConfig, Integer> selected = Utils.weightedRandomPick(spawnConfs);
            MobConfig mobConfig = selected.getKey();
            List<Integer> possibleLevels = MobConfig.parseLevels(mobConfig.spawn.levels);
            return spawnMobByConfig(mobConfig, location, Utils.randomPick(possibleLevels));
        } else {
            Collection<LevelConfig> values = InfPlugin.plugin.config().levelConfigs.values();
            List<WeightedPair<List<MobConfig>, Integer>> levelCandidates = new ArrayList<>();
            values.forEach(levelConfig -> {
                int from = levelConfig.spawnConfig.from;
                int to = levelConfig.spawnConfig.to;
                int level = levelConfig.level;
                int weight = levelConfig.spawnConfig.weight;
                World world = location.getWorld();
                if (world == null) {
                    throw new IllegalArgumentException();
                }
                double distance = location.distance(world.getSpawnLocation());
                if (distance < from || distance >= to) {
                    return;
                }
                List<MobConfig> collect = natualSpawnLists.get(level);
                if (collect == null) return;
                Biome biome = location.getBlock().getBiome();
                collect = collect.stream().parallel()
                        .filter(config1 -> config1.spawn.worlds.contains(world.getName()) && config1.spawn.biomes.contains(biome.name()))
                        .collect(Collectors.toList());
                if (!collect.isEmpty()) {
                    levelCandidates.add(new WeightedPair<>(collect, level, weight));
                }
            });
            if (!levelCandidates.isEmpty()) {
                WeightedPair<List<MobConfig>, Integer> selected = Utils.weightedRandomPick(levelCandidates);
                Integer level = selected.getValue();
                List<MobConfig> candidates = selected.getKey();
                MobConfig mobConfig = Utils.weightedRandomPick(candidates);
                return spawnMobByConfig(mobConfig, location, level);
            } else {
                return null;
            }
        }
    }

    public void registerMob(IMob mob) {
        World world = mob.getEntity().getWorld();
        uuidMap.put(mob.getEntity().getUniqueId(), mob);
        List<IMob> iMobs = worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
        iMobs.add(mob);
    }

    public void removeMob(IMob mob) {
        World world = mob.getEntity().getWorld();
        uuidMap.remove(mob.getEntity().getUniqueId());
        List<IMob> iMobs = worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
        iMobs.remove(mob);
        KeyedBossBar bossBar = mob.getBossBar();
        if (bossBar != null) {
            bossBar.setTitle(bossBar.getTitle().concat(InfPlugin.plugin.config().bossbar.killSuffix));
            new BukkitRunnable(){
                @Override
                public void run() {
                    bossBar.removeAll();
                }
            }.runTaskLater(InfPlugin.plugin, 20);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                mob.getEntity().remove();
            }
        }.runTaskLater(InfPlugin.plugin, 20);
    }

    public List<IMob> getMobsInWorld(World world) {
        return worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
    }

    public boolean isIMob(Entity entity) {
        return uuidMap.containsKey(entity.getUniqueId());
    }

    public IMob toIMob(Entity entity) {
        return uuidMap.get(entity.getUniqueId());
    }

    public void updateNearbyList(World world, int nearbyDistance) {
        final List<IMob> playerMobList = new ArrayList<>(uuidMap.size());
        Map<Player, List<IMob>> asyncMobsList = new LinkedHashMap<>();
        Map<IMob, List<Player>> asyncPlayersList = new LinkedHashMap<>();

        new ArrayList<>(worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>())).parallelStream()
                .forEach(iMob -> {
                    if (iMob.getEntity().isDead()){
                        return;
                    }
                    final List<Player> mobList = new ArrayList<>(uuidMap.size());
                    world.getPlayers().parallelStream()
                            .forEach(player -> {
                                if (iMob.getEntity().getLocation().distance(player.getLocation()) < nearbyDistance) {
                                    playerMobList.add(iMob);
                                    mobList.add(player);
                                } else {
                                    System.out.printf("should remove");
                                }
                                asyncMobsList.put(player, playerMobList);
                            });
                    asyncPlayersList.put(iMob, mobList);
                });
        playerNearbyList = asyncMobsList;
        mobNearbyList = asyncPlayersList;
    }

    public List<IMob> getMobsNearPlayer(Player player) {
        return ImmutableList.copyOf(playerNearbyList.computeIfAbsent(player, player1 -> new ArrayList<>()));
    }

    public List<Player> getPlayersNearMob(IMob iMob) {
        return ImmutableList.copyOf(mobNearbyList.computeIfAbsent(iMob, player1 -> new ArrayList<>()));
    }

    public void updateNearbyList(IMob iMob, int nearbyDistance) {
        List<Player> players = mobNearbyList.computeIfAbsent(iMob, iMob1 -> new ArrayList<>());
        World world = iMob.getEntity().getWorld();
        world.getPlayers().parallelStream()
                .forEach(player -> {
                    if (player.getLocation().distance(iMob.getEntity().getLocation()) < nearbyDistance){
                        players.add(player);
                        List<IMob> iMobs = playerNearbyList.computeIfAbsent(player, iMob1 -> new ArrayList<>());
                        iMobs.add(iMob);
                    }
                });
    }
}
