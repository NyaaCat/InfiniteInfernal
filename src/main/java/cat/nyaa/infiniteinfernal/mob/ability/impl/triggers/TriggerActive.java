package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityActive;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;

import java.util.Optional;

public class TriggerActive extends Trigger<AbilityActive, Void, Void> {
    @Override
    public Optional<Void> trigger(IMob mob, AbilityActive ability, Void event) {
        ability.active(mob);
        return Optional.empty();
    }

    @Override
    public Class<AbilityActive> getInterfaceType() {
        return AbilityActive.class;
    }
}
