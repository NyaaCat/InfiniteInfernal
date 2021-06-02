package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.api.ICondition;
import cat.nyaa.infiniteinfernal.mob.ability.trigger.Trigger;
import cat.nyaa.infiniteinfernal.utils.Weightable;
import org.bukkit.event.Event;

import java.util.List;

public interface IAbilitySet extends Weightable {
    List<IAbility> getAbilitiesInSet();
    <T> List<T> getAbilitiesInSet(Class<T> abilityClass);
    boolean checkConditions(IMob iMob);
    ICondition getCondition(String condition);

    boolean containsClass(Class<?> cls);

    boolean hasTrigger(Trigger trigger);

    <R, T, Evt extends Event> void trigger(IMob iMob, Class<?> abilityCls, Trigger<T, R, Evt> trigger, Evt event);
}
