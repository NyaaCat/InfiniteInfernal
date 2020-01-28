package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.infiniteinfernal.utils.Context;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import com.google.common.collect.ImmutableList;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
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

    final Map<UUID, IMob> uuidMap = new LinkedHashMap<>();
    Map<World, List<IMob>> worldMobMap = new LinkedHashMap<>();
    Map<Player, List<IMob>> playerNearbyList = new LinkedHashMap<>();
    Map<IMob, List<Player>> mobNearbyList = new LinkedHashMap<>();
    Map<BossBar, IMob> bossBarIMobMap = new LinkedHashMap<>();

    Map<String, MobConfig> nameCfgMap = new LinkedHashMap<>();
    Map<Integer, List<MobConfig>> natualSpawnLists = new LinkedHashMap<>();

    private MobManager() {
        this.load();
    }

    public void load() {
        initialize();
        NamedDirConfigs<MobConfig> mobConfigs = InfPlugin.plugin.config().mobConfigs;
        buildNatualSpawnList(mobConfigs);
        buildCfgMaps(mobConfigs);
    }

    private void initialize() {
//        if ((!worldMobMap.values().isEmpty())) {
//            worldMobMap.values().forEach(list -> {
//                if (!list.isEmpty()) {
//                    LinkedList<IMob> iMobs = new LinkedList<>(list);
//                    IMob poll;
//                    while ((poll = iMobs.poll()) != null) {
//                        removeMob(poll, false);
//                    }
//                }
//            });
//        }
//        worldMobMap.clear();
        nameCfgMap.clear();
        natualSpawnLists.clear();
    }

    private void buildCfgMaps(NamedDirConfigs<MobConfig> mobConfigs) {
        mobConfigs.values().stream()
                .forEach(config -> {
                    nameCfgMap.put(config.getName(), config);
                });
    }

    private void buildNatualSpawnList(NamedDirConfigs<MobConfig> mobConfigs) {
        mobConfigs.values().stream()
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
                    level = randomLevel(location);
                    if (level == null) return null;
                }
                CustomMob customMob = new CustomMob(config, level);
                Context.instance().put(MOB_SPAWN_CONTEXT, IS_IMOB, true);
                LivingEntity spawn = (LivingEntity) world.spawn(location, entityClass);
                spawn.addScoreboardTag("inf_infernal_mob");
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

    public boolean isMobBar(KeyedBossBar keyedBossBar) {
        return bossBarIMobMap.containsKey(keyedBossBar);
    }

    public Set<String> getMobConfigNames() {
        NamedDirConfigs<MobConfig> mobConfigs = InfPlugin.plugin.config().mobConfigs;
        return mobConfigs.keys();
    }

    public List<WeightedPair<MobConfig, Integer>> getSpawnableMob(Location location) {
        World world = location.getWorld();
        if (world == null) return new ArrayList<>();
        Biome biome = location.getBlock().getBiome();
        Config config = InfPlugin.plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(location);
        if (!regions.isEmpty()) {
            return getSpawnConfigsForRegion(regions, location);
        }
        List<WeightedPair<Integer, Integer>> validLevels = getValidLevels(location);
        List<WeightedPair<MobConfig, Integer>> spawnConfs = new ArrayList<>();
        validLevels.stream().forEach(pair -> {
            Integer level = pair.getKey();
            if (level != null) {
                List<MobConfig> collect = natualSpawnLists.get(level);
                FluidLocationWrapper fluidLocationWrapper = new FluidLocationWrapper(location);
                if (!collect.isEmpty()) {
                    collect.stream()
                            .filter(config1 -> {
                                List<String> biomes = config1.spawn.biomes;
                                List<String> worlds = config1.spawn.worlds;
                                return biomes != null && worlds != null
                                        && worlds.contains(world.getName()) && biomes.contains(biome.name());
                            })
                            .filter(mobConfig -> fluidLocationWrapper.isValid(mobConfig.type))
                            .forEach(mobConfig -> spawnConfs.add(new WeightedPair<>(mobConfig, mobConfig.getWeight(), mobConfig.getWeight())));
                }
            }
        });
        return spawnConfs;
    }

    public Collection<MobConfig> getMobConfigs() {
        return InfPlugin.plugin.config().mobConfigs.values();
    }

    public List<MobConfig> getMobsForLevel(int level) {
        return natualSpawnLists.get(level);
    }

    public Set<Integer> getLevels() {
        return natualSpawnLists.keySet();
    }

    public void initMobs() {
        Bukkit.getWorlds().forEach(world -> {
            if (InfPlugin.plugin.config().isEnabledInWorld(world)){
                world.getEntities().stream().filter(entity -> {
                    Set<String> scoreboardTags = entity.getScoreboardTags();
                    return scoreboardTags.contains("inf_infernal_mob") || scoreboardTags.contains("inf_damage_indicator");
                })
                        .forEach(Entity::remove);
            }
        });
    }

    static class FluidLocationWrapper {
        private static final List<EntityType> skyEntities = Arrays.asList(
                EntityType.PHANTOM,
                EntityType.BAT,
                EntityType.VEX,
                EntityType.BLAZE,
                EntityType.GHAST,
                EntityType.PARROT,
                EntityType.ENDER_DRAGON,
                EntityType.WITHER);

        private static final List<EntityType> waterEntities = Arrays.asList(
                EntityType.DROWNED,
                EntityType.GUARDIAN,
                EntityType.ELDER_GUARDIAN,
                EntityType.COD,
                EntityType.PUFFERFISH,
                EntityType.SALMON,
                EntityType.DOLPHIN,
                EntityType.TROPICAL_FISH,
                EntityType.SQUID,
                EntityType.TURTLE);
        private final Location location;

        FluidLocationWrapper(Location location) {
            this.location = location;
        }

        public boolean isValid(EntityType type) {
            if (location == null || type == null) return false;
            boolean isSky = checkSky(location);
            if (isSky) {
                return skyEntities.contains(type);
            }
            boolean isFluid = checkFluid(location);
            if (isFluid) {
                return waterEntities.contains(type);
            }
            return true;
        }

        private boolean checkFluid(Location location) {
            boolean result = true;
            Block block = location.getBlock();
            for (BlockFace value : BlockFace.values()) {
                result = result && block.getRelative(value).getType().equals(Material.WATER);
            }
            return result;
        }

        private boolean checkSky(Location location) {
            Block block = location.getBlock();
            Block d1 = block.getRelative(BlockFace.DOWN);
            return d1.getType().equals(Material.AIR);
        }
    }

    public IMob natualSpawn(Location location) {
        Config config = InfPlugin.plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(location);
        if (!regions.isEmpty()) {
            return spawnInRegion(regions, location);
        }
        Integer level = randomLevel(location);
        List<MobConfig> collect = natualSpawnLists.get(level);
        if (collect == null) return null;
        World world = location.getWorld();
        if (world == null) return null;
        Biome biome = location.getBlock().getBiome();
        FluidLocationWrapper fluidLocationWrapper = new FluidLocationWrapper(location);
        if (!collect.isEmpty()) {
            collect = collect.stream()
                    .filter(config1 -> {
                        List<String> biomes = config1.spawn.biomes;
                        List<String> worlds = config1.spawn.worlds;
                        return biomes != null && worlds != null
                                && worlds.contains(world.getName()) && biomes.contains(biome.name());
                    })
                    .filter(mobConfig -> fluidLocationWrapper.isValid(mobConfig.type))
                    .collect(Collectors.toList());
            MobConfig mobConfig = Utils.weightedRandomPick(collect);
            return spawnMobByConfig(mobConfig, location, level);
        }
        return null;
    }

    private Integer randomLevel(Location location) {
        List<WeightedPair<Integer, Integer>> levelCandidates = getValidLevels(location);
        WeightedPair<Integer, Integer> integerIntegerWeightedPair = Utils.weightedRandomPick(levelCandidates);
        return integerIntegerWeightedPair == null ? null : integerIntegerWeightedPair.getKey();
    }

    private List<WeightedPair<Integer, Integer>> getValidLevels(Location location) {
        Collection<LevelConfig> values = InfPlugin.plugin.config().levelConfigs.values();
        List<WeightedPair<Integer, Integer>> levelCandidates = new ArrayList<>();
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
            levelCandidates.add(new WeightedPair<>(level, level, weight));
        });
        return levelCandidates;
    }

    private IMob spawnInRegion(List<RegionConfig> regions, Location location) {
        List<WeightedPair<MobConfig, Integer>> spawnConfs = getSpawnConfigsForRegion(regions, location);
        if (!spawnConfs.isEmpty()) {
            WeightedPair<MobConfig, Integer> selected = Utils.weightedRandomPick(spawnConfs);
            if (selected == null) return null;
            MobConfig mobConfig = selected.getKey();
            return spawnMobByConfig(mobConfig, location, selected.getValue());
        }
        return null;
    }

    private List<WeightedPair<MobConfig, Integer>> getSpawnConfigsForRegion(List<RegionConfig> regions, Location location) {
        List<WeightedPair<MobConfig, Integer>> spawnConfs = new ArrayList<>();
        FluidLocationWrapper fluidLocationWrapper = new FluidLocationWrapper(location);
        regions.forEach(regionConfig -> {
            if (regionConfig.mobs.isEmpty()) {
                return;
            }
            regionConfig.mobs.forEach(mobs -> {
                try {
                    String[] split = mobs.split(":");
                    String mobId = split[0];
                    int mobWeight = Integer.parseInt(split[1]);
                    MobConfig mobConfig = nameCfgMap.get(mobId);
                    if (mobConfig == null) {
                        Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.spawn_no_id", mobs));
                        return;
                    }
                    World world = location.getWorld();
                    if (world != null && mobConfig.spawn.worlds.contains(world.getName())) {
                        Integer level = 0;
                        if (regionConfig.followGlobalLevel) {
                            level = randomLevel(location);
                        } else {
                            level = Utils.randomPick(MobConfig.parseLevels(mobConfig.spawn.levels));
                        }
                        if (level == null) return;
                        if (fluidLocationWrapper.isValid(mobConfig.type)) {
                            spawnConfs.add(new WeightedPair<>(mobConfig, level, mobWeight));
                        }
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.num_format", mobs));
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.bad_config", mobs));
                }
            });
        });
        return spawnConfs;
    }

    public void registerMob(IMob mob) {
        World world = mob.getEntity().getWorld();
        uuidMap.put(mob.getEntity().getUniqueId(), mob);
        bossBarIMobMap.put(mob.getBossBar(), mob);
        List<IMob> iMobs = worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
        iMobs.add(mob);
    }

    public void removeMob(IMob mob, boolean isKilled) {
        LivingEntity entity = mob.getEntity();
        if (entity == null) {
            worldMobMap.values().stream().forEach(iMobs -> {
                iMobs.remove(mob);
            });
            List<UUID> invalidIds = new ArrayList<>();
            uuidMap.forEach((uuid, iMob) -> {
                if (iMob.getEntity() == null) {
                    invalidIds.add(uuid);
                }
            });
            invalidIds.stream().forEach(uuidMap::remove);
            KeyedBossBar bossBar = mob.getBossBar();
            if (bossBar != null) {
                bossBar.removeAll();
                bossBarIMobMap.remove(mob.getBossBar(), mob);
            }
            return;
        }
        World world = entity.getWorld();
        uuidMap.remove(mob.getEntity().getUniqueId());
        List<IMob> iMobs = worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
        iMobs.remove(mob);
        if (isKilled) {
            KeyedBossBar bossBar = mob.getBossBar();
            if (bossBar != null) {
                bossBar.setProgress(0);
                bossBar.setColor(BarColor.RED);
                bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', mob.getTaggedName().concat(" ").concat(InfPlugin.plugin.config().bossbar.killSuffix)));
                new BukkitRunnable() {
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
                    bossBarIMobMap.remove(mob.getBossBar(), mob);
                }
            }.runTaskLater(InfPlugin.plugin, 20);
        } else {
            KeyedBossBar bossBar = mob.getBossBar();
            if (bossBar != null) {
                bossBar.removeAll();
                bossBarIMobMap.remove(mob.getBossBar(), mob);
            }
            Utils.removeEntityLater(mob.getEntity(), 20);
        }
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

    public void updateNearbyList() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        Map<Player, List<IMob>> asyncMobsList = new LinkedHashMap<>(players.size());
        Map<IMob, List<Player>> asyncPlayersList = new LinkedHashMap<>(uuidMap.size());
        ArrayList<IMob> iMobs;
        synchronized (uuidMap) {
            iMobs = new ArrayList<>(uuidMap.values());
        }
        iMobs.stream()
                .forEach(iMob -> {
                    if (iMob.getEntity().isDead()) {
                        return;
                    }
                    players.stream().forEach(player -> {
                        if (!iMob.getEntity().getWorld().equals(player.getWorld())) {
                            return;
                        }
                        World world = player.getWorld();
                        WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(world.getName());
                        double nearbyDistance = 128;
                        if (worldConfig != null) {
                            nearbyDistance = Math.max(worldConfig.aggro.range.max * 1.5, worldConfig.spawnRangeMax * 2);
                        }
                        if (iMob.getEntity().getLocation().distance(player.getLocation()) < nearbyDistance) {
                            List<IMob> mobList = asyncMobsList.computeIfAbsent(player, player1 -> new ArrayList<>());
                            List<Player> playerMobList = asyncPlayersList.computeIfAbsent(iMob, iMob1 -> new ArrayList<>());
                            playerMobList.add(player);
                            mobList.add(iMob);
                        }
                    });
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
        world.getPlayers().forEach(player -> {
            if (player.getLocation().distance(iMob.getEntity().getLocation()) < nearbyDistance) {
                players.add(player);
                List<IMob> iMobs = playerNearbyList.computeIfAbsent(player, iMob1 -> new ArrayList<>());
                iMobs.add(iMob);
            }
        });
    }
}
