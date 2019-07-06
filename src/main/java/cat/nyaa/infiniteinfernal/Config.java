package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Config extends PluginConfigure {
    InfPlugin plugin;
    Config(InfPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Serializable
    public String language = "en_US";

    @Serializable
    public String nameTag = "[INFERNAL] {level.prefix} {mob.name} Level {Level.level}";

    @Serializable
    public BossbarConfig bossbar = new BossbarConfig();

    @Serializable
    public List<String> tags = new ArrayList<>();

    @Serializable
    public Map<String, WorldConfig> worlds = new LinkedHashMap<>();

    //<STANDALONE CONFIGS>
    public DirConfigs<AbilitySetConfig> abilityConfigs;
    public DirConfigs<LevelConfig> levelConfigs;
    public DirConfigs<MobConfig> mobConfigs;
    public DirConfigs<RegionConfig> regionConfigs;

    private void saveStandaloneConfigs() {
        abilityConfigs.saveToDir();
        levelConfigs.saveToDir();
        mobConfigs.saveToDir();
        regionConfigs.saveToDir();
    }

    private void loadStandaloneConfigs() {
        abilityConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "abilities/"), AbilitySetConfig.class);
        levelConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "level/"), LevelConfig.class);
        mobConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "mobs/"), MobConfig.class);
        regionConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "regions/"), RegionConfig.class);
        abilityConfigs.loadFromDir();
        levelConfigs.loadFromDir();
        mobConfigs.loadFromDir();
        regionConfigs.loadFromDir();
    }
    //<STANDALONE CONFIGS/>

    @Override
    public void load() {
        super.load();
        this.loadStandaloneConfigs();
    }

    @Override
    public void save() {
        super.save();
        this.saveStandaloneConfigs();
    }

    public List<RegionConfig> getRegionsForLocation(Location location) {
        return regionConfigs.values().stream()
                .filter(regionConfig1 -> regionConfig1.region.contains(location))
                .collect(Collectors.toList());
    }
}
