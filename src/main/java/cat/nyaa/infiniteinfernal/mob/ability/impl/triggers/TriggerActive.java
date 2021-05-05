package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.event.MobTickEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityActive;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;

import java.util.Optional;

public class TriggerActive extends Trigger<AbilityActive, Void, MobTickEvent> {
    public TriggerActive(String name) {
        super(name);
    }

    @Override
    public Optional<Void> trigger(IMob mob, AbilityActive ability, MobTickEvent event) {
        ability.active(mob);
        return Optional.empty();
    }

    @Override
    public Class<AbilityActive> getInterfaceType() {
        return AbilityActive.class;
    }
}
