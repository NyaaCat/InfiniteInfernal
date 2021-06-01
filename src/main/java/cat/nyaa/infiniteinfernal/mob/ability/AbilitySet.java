package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;
import cat.nyaa.infiniteinfernal.mob.ability.trigger.Trigger;
import cat.nyaa.infiniteinfernal.mob.ability.trigger.TriggeringMode;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AbilitySet implements IAbilitySet {
    private List<IAbility> abilities;
    private int weight;
    public List<String> trigger;
    public TriggeringMode triggeringMode;
    //todo add possibility and cooldown

    public AbilitySet(AbilitySetConfig config){
        weight = config.weight;
        abilities = new ArrayList<>();
        config.abilities.values().stream().forEach(iAbility -> {
            try {
                IAbility clone= iAbility.getClass().getConstructor().newInstance();
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                iAbility.serialize(yamlConfiguration);
                clone.deserialize(yamlConfiguration);
                abilities.add(clone);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        this.trigger = new ArrayList<>(Arrays.asList(config.trigger.split(",")));
        this.triggeringMode = config.triggeringMode;
    }

    @Override
    public List<IAbility> getAbilitiesInSet() {
        return abilities;
    }

    @Override
    public <T> List<T> getAbilitiesInSet(Class<T> abilityClass) {
        return abilities.stream().filter(iAbility -> abilityClass.isAssignableFrom(iAbility.getClass()))
                .map(iAbility -> ((T) iAbility)).collect(Collectors.toList());
    }

    @Override
    public boolean containsClass(Class<?> cls) {
        return abilities.stream().anyMatch(iAbility -> cls.isAssignableFrom(iAbility.getClass()));
    }

    @Override
    public boolean hasTrigger(Trigger trigger) {
        return false;
    }

    @Override
    public int getWeight() {
        return weight;
    }
}
