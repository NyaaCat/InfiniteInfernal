package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.IAbility;
import cat.nyaa.infiniteinfernal.mob.ability.TriggeringMode;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AbilitySetConfig extends NamedFileConfig {
    private InfPlugin plugin;

    public AbilitySetConfig(String name) {
        super(name);
    }

    @Serializable(name = "abilities", manualSerialization = true)
    public Map<String, IAbility> abilities = new LinkedHashMap<>();
    @Serializable
    public int weight = 10;
    @Serializable(name = "trigger")
    public String trigger = "passive";
    @Serializable(name = "triggeringMode")
    public TriggeringMode triggeringMode = TriggeringMode.RANDOM;

    @Override
    public void deserialize(ConfigurationSection config) {
        super.deserialize(config);
        ConfigurationSection abilities = config.getConfigurationSection("abilities");
        Set<String> keys = abilities.getKeys(false);
        keys.forEach(s -> {

        });
    }

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    protected String getFileDirName() {
        return "abilities";
    }

    public int getWeight() {
        return weight;
    }
}
