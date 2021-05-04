package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.utils.Weightable;

import java.util.List;

public interface IAbilitySet extends Weightable {
    List<IAbility> getAbilitiesInSet();
    <T> List<T> getAbilitiesInSet(Class<T> abilityClass);

    boolean containsClass(Class<?> cls);
    boolean containsActive();
    boolean containsPassive();
    boolean containsDummy();
}
