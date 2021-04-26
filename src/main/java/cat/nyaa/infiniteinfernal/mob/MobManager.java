package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.configs.NamedDirConfigs;
import cat.nyaa.infiniteinfernal.configs.RegionConfig;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import cat.nyaa.infiniteinfernal.utils.context.Context;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
    Map<String, List<MobConfig>> natualSpawnLists = new LinkedHashMap<>();

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
                    List<String> validLevels = config.getSpawnLevels();
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

    public IMob spawnMobByConfig(MobConfig config, Location location, String level) {
        if (config == null) return null;
        EntityType entityType = config.type;
        World world = location.getWorld();
        if (world != null) {
            Class<? extends Entity> entityClass = entityType.getEntityClass();
            if (entityClass != null && LivingEntity.class.isAssignableFrom(entityClass)) {
                CustomMob customMob = new CustomMob(config, level);
                Context.instance().put(MOB_SPAWN_CONTEXT, IS_IMOB, true);
                LivingEntity spawn = (LivingEntity) world.spawn(location, entityClass);
                Context.instance().removeTemp(MOB_SPAWN_CONTEXT, IS_IMOB);
                customMob.makeInfernal(spawn);
                InfPlugin.plugin.config().tags.forEach(spawn::addScoreboardTag);
                registerMob(customMob);
                bossBarIMobMap.put(customMob.getBossBar(), customMob);
                if (customMob.isDynamicHealth()){
                    customMob.tweakHealth();
                }
                return customMob;
            }
        }
        return null;
    }

    public Collection<IMob> getMobs() {
        return Collections.unmodifiableCollection(uuidMap.values());
    }

    public IMob spawnMobByName(String name, Location location, String level) {
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

    public List<WeightedPair<MobConfig, String>> getSpawnableMob(Location location) {
        World world = location.getWorld();
        if (world == null) return new ArrayList<>();
        Config config = InfPlugin.plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(location);
        if (!regions.isEmpty()) {
            return getSpawnConfigsForRegion(regions, location);
        }
        //todo default spawn logic
        return null;
    }

    public Collection<MobConfig> getMobConfigs() {
        return InfPlugin.plugin.config().mobConfigs.values();
    }

    public List<MobConfig> getMobsForLevel(String level) {
        return natualSpawnLists.get(level);
    }

    public Set<String> getLevels() {
        return natualSpawnLists.keySet();
    }

    public void initMobs() {
        Bukkit.getWorlds().forEach(world -> {
            if (InfPlugin.plugin.config().isEnabledInWorld(world)){
                world.getEntities().stream().filter(entity -> {
                    Set<String> scoreboardTags = entity.getScoreboardTags();
                    List<String> tags = InfPlugin.plugin.config().tags;
                    return scoreboardTags.stream().anyMatch(tags::contains) || scoreboardTags.contains("inf_damage_indicator");
                })
                        .forEach(Entity::remove);
            }
        });
    }

    public TargetDummy spawnTargetDummy(String mobName, Location location, String level) {
        MobConfig config = nameCfgMap.get(mobName);
        if (config == null) return null;
        EntityType entityType = config.type;
        World world = location.getWorld();
        if (world != null) {
            Class<? extends Entity> entityClass = entityType.getEntityClass();
            if (entityClass != null && LivingEntity.class.isAssignableFrom(entityClass)) {

                Context.instance().put(MOB_SPAWN_CONTEXT, IS_IMOB, true);
                TargetDummy customMob = new TargetDummy(config, location);
                Context.instance().removeTemp(MOB_SPAWN_CONTEXT, IS_IMOB);
                customMob.respawn();
                InfPlugin.plugin.config().tags.forEach(customMob.getEntity()::addScoreboardTag);
                registerMob(customMob);
                if (customMob.isDynamicHealth()){
                    customMob.tweakHealth();
                }
                return customMob;
            }
        }
        return null;
    }

    public static class FluidLocationWrapper {
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

        public static boolean isSkyMob(EntityType entityType){
            return skyEntities.contains(entityType);
        }

        public static boolean isWaterMob(EntityType entityType){
            return waterEntities.contains(entityType);
        }
    }

    public Pair<MobConfig, String> selectNatualMobConfig(Location center){
        String level = null;
        //todo build level
        World world = center.getWorld();
        if (world == null) return null;
        final List<MobConfig> mobConfigs = natualSpawnLists.get(level);
        if (mobConfigs == null) {
            return null;
        }

        Biome biome = center.getBlock().getBiome();
        List<MobConfig> collect = mobConfigs.stream()
                //todo implement biomes
                .filter(config1 -> {
                    return true;
                }).collect(Collectors.toList());

        MobConfig mobConfig = RandomUtil.weightedRandomPick(collect);
        return Pair.of(mobConfig, level);
    }

    public IMob natualSpawn(Location location) {
        Config config = InfPlugin.plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(location);
        if (!regions.isEmpty()) {
            return spawnInRegion(regions, location);
        }

        Pair<MobConfig, String> mobConfigIntegerPair = selectNatualMobConfig(location);
        String level = mobConfigIntegerPair.getValue();
        List<MobConfig> collect = natualSpawnLists.get(level);
        if (collect == null) return null;
        if (!collect.isEmpty()) {
            return spawnMobByConfig(mobConfigIntegerPair.getKey(), location, level);
        }
        return null;
    }

    public IMob spawnInRegion(List<RegionConfig> regions, Location center) {
        WeightedPair<MobConfig, String> mobConfig = selectConfigInRegion(regions, center);
        return spawnMobByConfig(mobConfig.getKey(), center, mobConfig.getValue());
    }

    public WeightedPair<MobConfig, String> selectConfigInRegion(List<RegionConfig> regions, Location center){
        List<WeightedPair<MobConfig, String>> spawnConfs = getSpawnConfigsForRegion(regions, center);
        if (!spawnConfs.isEmpty()) {
            WeightedPair<MobConfig, String> selected = RandomUtil.weightedRandomPick(spawnConfs);
            if (selected == null) return null;
            return selected;
        }
        return null;
    }

    private List<WeightedPair<MobConfig, String>> getSpawnConfigsForRegion(List<RegionConfig> regions, Location location) {
        //todo finish string level for this
        List<WeightedPair<MobConfig, String>> spawnConfs = new ArrayList<>();
        Biome biome = location.getBlock().getBiome();
        regions.forEach(regionConfig -> {
            if (regionConfig.mobs.isEmpty()) {
                return;
            }
            final String level = regionConfig.getLevel();
            regionConfig.mobs.forEach(mobs -> {
                try {
                    String[] split = mobs.split(":");
                    String mobId = split[0];
                    int mobWeight = Integer.parseInt(split[1]);
                    MobConfig mobConfig = nameCfgMap.get(mobId);
                    if (mobConfig == null) {
                        Bukkit.getLogger().log(Level.WARNING, I18n.format("error.mob.spawn_no_id", mobs));
                        return;
                    }
                    World world = location.getWorld();
                    if (world != null && mobConfig.spawn.worlds.contains(world.getName())) {
                        spawnConfs.add(new WeightedPair<>(mobConfig, level, mobWeight));
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
        List<IMob> iMobs = worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
        iMobs.add(mob);

        final Config config = InfPlugin.plugin.config();
        MobManager.instance().updateNearbyList(mob, config.spawnRangeMax + 48);
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
                bossBar.setTitle(HexColorUtils.hexColored( mob.getTaggedName().concat(" ").concat(InfPlugin.plugin.config().bossbar.killSuffix)));
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
                        final Config config = InfPlugin.plugin.config();
                        double nearbyDistance = 128;
                        nearbyDistance = Math.max(config.aggroRangeMax * 1.5, config.spawnRangeMax * 2);

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
