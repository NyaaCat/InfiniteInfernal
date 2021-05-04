package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityLocation;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;

import java.util.Optional;

public class TriggerLocation extends Trigger<AbilityLocation, Void, MobCastEvent> {
    @Override
    public Optional<Void> trigger(IMob iMob, AbilityLocation ability, MobCastEvent event) {
        ability.fire(iMob, event);
        return Optional.empty();
    }

    @Override
    public Class<AbilityLocation> getInterfaceType() {
        return AbilityLocation.class;
    }
}
