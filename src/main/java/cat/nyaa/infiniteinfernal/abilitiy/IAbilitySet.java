package cat.nyaa.infiniteinfernal.abilitiy;

import cat.nyaa.infiniteinfernal.utils.Weightable;

import java.util.List;

public interface IAbilitySet extends Weightable {
    List<IAbility> getAbilitiesInSet();
    <T> List<T> getAbilitiesInSet(Class<T> abilityClass);

    boolean containsActive();
    boolean containsPassive();
    boolean containsDummy();
}
