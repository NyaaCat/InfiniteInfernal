package cat.nyaa.infiniteinfernal.mob.ability.trigger;

import cat.nyaa.infiniteinfernal.mob.ability.AbilitySet;
import cat.nyaa.infiniteinfernal.mob.ability.IAbility;
import org.bukkit.event.Event;

import java.util.Queue;

public class TriggeredAbilitySet<T extends Event> {
    Queue<IAbility> queue;
    final T triggeredEvent;

    public TriggeredAbilitySet(AbilitySet abilitySet, T event){
        triggeredEvent = event;
    }


}
