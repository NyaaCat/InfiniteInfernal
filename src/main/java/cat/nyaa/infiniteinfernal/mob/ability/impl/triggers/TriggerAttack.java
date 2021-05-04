package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;

import java.util.Optional;

public class TriggerAttack extends Trigger<AbilityAttack, Void, Void> {
    @Override
    public Optional<Void> trigger(IMob iMob, AbilityAttack ability, Void event) {
        return Optional.empty();
    }

    @Override
    public Class<AbilityAttack> getInterfaceType() {
        return AbilityAttack.class;
    }
}
