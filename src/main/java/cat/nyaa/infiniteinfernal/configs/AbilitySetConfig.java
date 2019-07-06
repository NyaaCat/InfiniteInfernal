package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.abilitiy.IAbility;

import java.util.LinkedHashMap;
import java.util.Map;

public class AbilitySetConfig extends IdFileConfig {
    private InfPlugin plugin;

    public AbilitySetConfig(int id) {
        super(id);
    }

    @Serializable
    Map<String, IAbility> abilities = new LinkedHashMap<>();

    @Override
    public String getPrefix() {
        return "set";
    }

}
