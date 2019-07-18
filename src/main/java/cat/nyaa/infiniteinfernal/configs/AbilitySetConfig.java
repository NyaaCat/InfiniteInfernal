package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.IAbility;

import java.util.LinkedHashMap;
import java.util.Map;

public class AbilitySetConfig extends IdFileConfig {
    private InfPlugin plugin;

    public AbilitySetConfig(int id) {
        super(id);
    }

    @Serializable
    public Map<String, IAbility> abilities = new LinkedHashMap<>();
    @Serializable
    public int weight = 10;

    @Override
    public String getPrefix() {
        return "set";
    }

    @Override
    public String getDir() {
        return "abilities";
    }

    public int getWeight() {
        return weight;
    }
}
