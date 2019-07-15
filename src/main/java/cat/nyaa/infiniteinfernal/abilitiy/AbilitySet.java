package cat.nyaa.infiniteinfernal.abilitiy;

import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AbilitySet implements IAbilitySet {
    private List<IAbility> abilities;
    private int weight;

    public AbilitySet(AbilitySetConfig config){
        weight = config.weight;
        abilities = new ArrayList<>(config.abilities.values());
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
        return abilities.stream().anyMatch(iAbility -> iAbility.getClass().isAssignableFrom(AbilityActive.class));
    }

    @Override
    public boolean containsPassive() {
        return abilities.stream().anyMatch(iAbility -> iAbility.getClass().isAssignableFrom(AbilityPassive.class));
    }

    @Override
    public boolean containsDummy() {
        return abilities.stream().anyMatch(iAbility -> iAbility.getClass().isAssignableFrom(AbilityDummy.class));
    }

    @Override
    public int getWeight() {
        return weight;
    }
}
