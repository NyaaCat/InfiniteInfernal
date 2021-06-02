package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.ability.api.ICondition;
import cat.nyaa.infiniteinfernal.mob.ability.trigger.Trigger;
import cat.nyaa.infiniteinfernal.utils.Weightable;

import java.util.List;

public interface IAbilitySet extends Weightable {
    List<IAbility> getAbilitiesInSet();
    <T> List<T> getAbilitiesInSet(Class<T> abilityClass);
    boolean hasCondition(String condition);
    ICondition getCondition(String condition);

    boolean containsClass(Class<?> cls);

    boolean hasTrigger(Trigger trigger);
}
