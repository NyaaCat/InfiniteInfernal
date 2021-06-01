package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityManager;
import cat.nyaa.infiniteinfernal.mob.ability.IAbility;
import cat.nyaa.infiniteinfernal.mob.ability.TriggeringMode;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

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
    public String trigger = "ACTIVE";
    @Serializable(name = "triggeringMode")
    public TriggeringMode triggeringMode = TriggeringMode.RANDOM;

    @Override
    public void deserialize(ConfigurationSection config) {
        super.deserialize(config);
        ConfigurationSection abilities = config.getConfigurationSection("abilities");
        Set<String> keys = abilities.getKeys(false);
        keys.forEach(s -> {
            IAbility iAbility = newAbilityInstance(s);
            this.abilities.put(s, iAbility);
        });
    }

    private static final List<Character> digits = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
    private IAbility newAbilityInstance(String s) {
        String className;
        final int splitPoint = s.lastIndexOf("-");
        if (splitPoint == -1){
            s = stripDigits(s);
            className = s;
        }else {
            className = s.substring(0, splitPoint);
        }
        String abilityName = Character.toUpperCase(className.charAt(0)) + className.substring(1).toLowerCase();
        return AbilityManager.copyOf(abilityName);
    }

    private String stripDigits(String s) {
        while (digits.contains(s.charAt(s.length()-1))){
            s = s.substring(0, s.length()-1);
        }
        return s;
    }

    @Override
    public void serialize(ConfigurationSection config) {
        super.serialize(config);
        ConfigurationSection aSec = config.createSection("abilities");
        abilities.forEach((name, ability) -> {
            ConfigurationSection bSec = aSec.createSection(name);
            ability.serialize(bSec);
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
