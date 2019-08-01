package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.IAbility;

import java.util.LinkedHashMap;
import java.util.Map;

public class AbilitySetConfig extends NamedFileConfig {
    private InfPlugin plugin;

    public AbilitySetConfig(String name) {
        super(name);
    }


    @Serializable
    public Map<String, IAbility> abilities = new LinkedHashMap<>();
    @Serializable
    public int weight = 10;

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
