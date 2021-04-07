package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.IAbility;
import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.infiniteinfernal.utils.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Config extends PluginConfigure {
    InfPlugin plugin;

    Config(InfPlugin plugin) {
        this.plugin = plugin;
        abilityConfigs = new NamedDirConfigs<>(new File(plugin.getDataFolder(), "abilities"), AbilitySetConfig.class);
        levelConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "levels"), LevelConfig.class);
        mobConfigs = new NamedDirConfigs<>(new File(plugin.getDataFolder(), "mobs"), MobConfig.class);
        regionConfigs = new NamedDirConfigs<>(new File(plugin.getDataFolder(), "regions"), RegionConfig.class);
        addEffects = new LinkedHashMap<>();
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Serializable
    public String language = "en_US";

    @Serializable
    public String nameTag = "[INFERNAL] {level.prefix} {mob.type} Level {level.level}";

    @Serializable
    public BossbarConfig bossbar = new BossbarConfig();

    @Serializable
    public List<String> tags = new ArrayList<>();

    @Serializable
    public int groupShareRange = 50;

    @Serializable
    public boolean enableActionbarInfo = true;

    @Serializable
    public int groupCapacity= 8;

    @Serializable(name = "player.base.mana")
    public double defaultMana = 20;

    @Serializable(name = "player.base.rage")
    public double defaultRage = 100;

    @Serializable(name = "GetDropMessageFeedback")
    public boolean isGetDropMessageEnabled = false;

    @Serializable
    public Map<String, String> addEffects;

    @Serializable
    public boolean enabled = true;

    @Serializable(name = "dps.title")
    public String dpsTitle = "&aDPS: &c{dps} &atotal: &c{total} &amax: &c{max}";

    @Serializable(name = "dps.default.health")
    public double dpsDefaultHealth = 2048;

    @Serializable(name = "dps.refresh_interval")
    public int dpsRefreshInterval = 200;

    @Serializable(name = "dps.entity_tag")
    public String dpsTag = "";

    @Serializable(name = "mobParticle")
    public ParticleConfig mobParticle;

    @Serializable
    public List<String> enabledWorld = new ArrayList<>();
    @Serializable
    public List<String> disableNaturalSpawning = new ArrayList<>();

    @Serializable(name = "max-mob-per-player")
    public int maxMobPerPlayer = 10;

    @Serializable(name = "max-mob-in-world")
    public int maxMobInWorld = 240;

    @Serializable(name = "spawn-range-min")
    public int spawnRangeMin = 60;

    @Serializable(name = "spawn-range-max")
    public int spawnRangeMax = 120;

    @Serializable(name = "spawn-interval")
    public int mobSpawnInteval = 20;

    @Serializable(name = "mob-active-interval")
    public int mobTickInterval = 60;

    @Serializable(name = "spawn.light.sky.min")
    public int spawnMinSkyLight = 0;

    @Serializable(name = "spawn.light.sky.max")
    public int spawnMaxSkyLight = 0xf;

    @Serializable(name = "spawn.light.block.min")
    public int spawnMinBlockLight = 0;

    @Serializable(name = "spawn.light.block.max")
    public int spawnMaxBlockLight = 0xf;

    @Serializable(name = "spawn.light.total.min")
    public int spawnMinLight = 0;

    @Serializable(name = "spawn.light.total.max")
    public int spawnMaxLight = 0xf;

    @Serializable(name = "aggro.range.max")
    public double aggroRangeMax = 128;
    @Serializable(name = "aggro.range.min")
    public double aggroRangeMin = 10;

    @Serializable(name = "aggro.base")
    public int aggroBase = 10;

    @Serializable(name = "aggro.modifiers.dec")
    public String aggroDec = "effect:INVISIBILITY:2";
    @Serializable(name = "aggro.modifiers.inc")
    public String aggroInc = "attribute:GENERIC_LUCK:-2";

    @Serializable(name = "loot.global")
    public int lootGlobal = 70;
    @Serializable(name = "loot.modifiers.overall.inc")
    public List<String> lootOverallInc = new ArrayList<>();
    @Serializable(name = "loot.modifiers.overall.dec")
    public List<String> lootOverallDec = new ArrayList<>();
    @Serializable(name = "loot.modifiers.overall.max")
    public double lootOverallMax = 100;
    @Serializable(name = "loot.modifiers.dynamic.inc")
    public List<String> lootDynamicInc = new ArrayList<>();
    @Serializable(name = "loot.modifiers.dynamic.dec")
    public List<String> lootDynamicDec = new ArrayList<>();
    @Serializable(name = "loot.modifiers.overall.max")
    public double lootDynamicMax = 100;

    {
        String inc = "attribute:GENERIC_LUCK:10";
        String dec = "effect:UNLUCK:5";
        lootOverallInc.add(inc);
        lootOverallDec.add(dec);
        lootDynamicInc.add(inc);
        lootDynamicDec.add(dec);
    }

    @Serializable(name = "friendly_fire_punish.enabled")
    public boolean friendlyFirePunishEnabled = false;
    @Serializable(name = "friendly_fire_punish.debuff")
    public String friendlyFirePunishDebuff = "UNLUCK:4:600";

    @Serializable(name = "broadcast.default")
    public BroadcastMode defaultBroadcastMode = BroadcastMode.NEARBY;
    @Serializable(name = "broadcast.range")
    public int broadcastRange = 160;

    @Serializable
    public boolean enableTrueDamage = true;

    @Serializable
    public double despawnRange = 128;

    @Serializable
    public Map<String, Double> trueDamage = new HashMap<>();

    {
        trueDamage.put(EntityDamageEvent.DamageCause.WITHER.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.POISON.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.FIRE_TICK.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.FIRE.name().toLowerCase(), 2d);
        trueDamage.put(EntityDamageEvent.DamageCause.LAVA.name().toLowerCase(), 4d);
        trueDamage.put(EntityDamageEvent.DamageCause.LIGHTNING.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.FIRE_TICK.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.MAGIC.name().toLowerCase(), -1.01d);
        trueDamage.put(EntityDamageEvent.DamageCause.SUFFOCATION.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.VOID.name().toLowerCase(), 4d);
        trueDamage.put(EntityDamageEvent.DamageCause.DROWNING.name().toLowerCase(), 2d);
        trueDamage.put(EntityDamageEvent.DamageCause.HOT_FLOOR.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.STARVATION.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.DRAGON_BREATH.name().toLowerCase(), 1d);
        trueDamage.put(EntityDamageEvent.DamageCause.CONTACT.name().toLowerCase(), 1d);
    }

    public double getTruedamage(String type) {
        return trueDamage.getOrDefault(type, 0d);
    }

    public boolean isTrueDamageEnabled() {
        return enableTrueDamage;
    }

    {
        mobParticle = new ParticleConfig();
        mobParticle.type = Particle.LAVA;
        mobParticle.amount = 10;
    }

    //<STANDALONE CONFIGS>
    public NamedDirConfigs<AbilitySetConfig> abilityConfigs;
    public DirConfigs<LevelConfig> levelConfigs;
    public NamedDirConfigs<MobConfig> mobConfigs;
    public NamedDirConfigs<RegionConfig> regionConfigs;

    private void saveStandaloneConfigs() {
        abilityConfigs.saveToDir();
        levelConfigs.saveToDir();
        mobConfigs.saveToDir();
        regionConfigs.saveToDir();
    }

    private void loadStandaloneConfigs() {
        abilityConfigs.loadFromDir();
        levelConfigs.loadFromDir();
        mobConfigs.loadFromDir();
        regionConfigs.loadFromDir();
    }
    //<STANDALONE CONFIGS/>

    @Override
    public void load() {
        getPlugin().saveDefaultConfig();
        getPlugin().reloadConfig();
        deserialize(getPlugin().getConfig());
        abilityConfigs.clear();
        levelConfigs.clear();
        mobConfigs.clear();
        regionConfigs.clear();
        addEffectInstance = null;
        this.loadStandaloneConfigs();
        save();
    }

    {
        tags.add("[infernal]");
        for (int i = 0; i < 12; i++) {
            LevelConfig config = new LevelConfig(i);
            config.level = i;
            config.spawnWeight = i;
            config.spawnFrom = 200 * i;
            config.spawnTo = 200 * (i + 1);
            config.aggro = 10 + 0.5 * i;
            config.damage = 20 * i;
            config.damageResist = 0;
            config.exp = 10 * i;
            config.health = 20 * i;
            config.prefix = "level " + i;
            levelConfigs.add(config);
        }
        addEffects.put("target_lost", "effect:BLINDNESS:10");
        addEffects.put("disorder", "effect:CONFUSION:10");
        addEffects.put("dementia", "effect:SLOW_DIGGING:10");
    }

    private void addAbilities(AbilitySetConfig actives, Class<? extends IAbility>[] activeClasses) {
        if (activeClasses.length > 0) {
            for (Class<? extends IAbility> abilityClass : activeClasses) {
                try {
                    Constructor<? extends IAbility> constructor = abilityClass.getConstructor();
                    IAbility o = constructor.newInstance();
                    actives.abilities.put(o.getName(), o);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void save() {
        super.save();
        this.saveStandaloneConfigs();
    }

    public List<RegionConfig> getRegionsForLocation(Location location) {
        return regionConfigs.values().stream()
                .filter(regionConfig1 -> regionConfig1.region != null && regionConfig1.region.contains(location))
                .collect(Collectors.toList());
    }

    private Map<String, ICorrector> addEffectInstance = null;

    public Map<String, ICorrector> getAddEffects() {
        if (addEffectInstance == null) {
            initAddEffectInstance();
        }
        return addEffectInstance;
    }

    public ICorrector getAddEffect(String name) {
        if (addEffectInstance == null) {
            initAddEffectInstance();
        }
        return addEffectInstance.get(name);
    }

    public boolean isEnabledInWorld(World world){
        return enabled && enabledWorld.contains(world.getName());
    }

    private void initAddEffectInstance() {
        addEffectInstance = new LinkedHashMap<>();
        addEffects.forEach(((s, s2) -> {
            ICorrector iCorrector = CorrectionParser.parseStr(s);
            if (iCorrector != null) {
                addEffectInstance.put(s, iCorrector);
            }
        }));
    }

    private ICorrector aggroDecCorrector;
    private ICorrector aggroIncCorrector;

    public ICorrector getAggroDec() {
        if (aggroDecCorrector == null){
            aggroDecCorrector = CorrectionParser.parseStr(aggroDec);
        }
        return aggroDecCorrector;
    }

    public ICorrector getAggroInc(){
        if (aggroIncCorrector == null){
            aggroIncCorrector = CorrectionParser.parseStr(aggroInc);
        }
        return aggroIncCorrector;
    }

    public void clearCache(){
        aggroDecCorrector = null;
        aggroIncCorrector = null;

    }

    public List<World> getEnabledWorlds() {
        return Bukkit.getWorlds().stream().filter(world -> enabledWorld.contains(world.getName()))
                .collect(Collectors.toList());
    }

    public boolean isAutoSpawnDisabledInWorld(World world){
        return enabled && disableNaturalSpawning.contains(world.getName());
    }
}
