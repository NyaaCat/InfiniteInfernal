package cat.nyaa.infiniteinfernal.ability;

import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AbilitySet implements IAbilitySet {
    private List<IAbility> abilities;
    private int weight;

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
    public boolean containsActive() {
        return abilities.stream().anyMatch(iAbility -> AbilityActive.class.isAssignableFrom(iAbility.getClass()));
    }

    @Override
    public boolean containsPassive() {
        return abilities.stream().anyMatch(iAbility -> AbilityPassive.class.isAssignableFrom(iAbility.getClass()) || AbilityAttack.class.isAssignableFrom(iAbility.getClass()));
    }

    @Override
    public boolean containsDummy() {
        return abilities.stream().anyMatch(iAbility -> AbilityDummy.class.isAssignableFrom(iAbility.getClass()));
    }

    @Override
    public int getWeight() {
        return weight;
    }
}
