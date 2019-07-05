package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;
import cat.nyaa.infiniteinfernal.configs.LevelConfig;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public List<String> tags = new ArrayList<>();

    @Serializable
    public Map<String, WorldConfig> worlds = new LinkedHashMap<>();

    //<STANDALONE CONFIGS>
    public List<LevelConfig> levelConfigs = new ArrayList<>();
    public List<AbilitySetConfig> abilities = new ArrayList<>();

    private void saveStandaloneConfigs() {

    }

    private void loadStandaloneConfigs() {

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


}
