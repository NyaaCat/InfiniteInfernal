package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.event.MobNearDeathEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityNearDeath;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;

import java.util.Optional;

public class TriggerNeardeath extends Trigger<AbilityNearDeath, Void, MobNearDeathEvent> {
    public TriggerNeardeath(String name) {
        super(name);
    }

    @Override
    public Optional<Void> trigger(IMob iMob, AbilityNearDeath ability, MobNearDeathEvent event) {
        ability.onDeath(iMob, event);
        return Optional.empty();
    }

    @Override
    public Class<AbilityNearDeath> getInterfaceType() {
        return AbilityNearDeath.class;
    }
}
