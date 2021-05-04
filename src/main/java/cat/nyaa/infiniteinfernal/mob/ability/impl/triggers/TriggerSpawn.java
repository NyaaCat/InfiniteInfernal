package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.event.MobSpawnEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;

import java.util.Optional;

public class TriggerSpawn extends Trigger<AbilitySpawn, Void, MobSpawnEvent> {
    @Override
    public Optional<Void> trigger(IMob iMob, AbilitySpawn ability, MobSpawnEvent event) {
        ability.onSpawn(iMob);
        return Optional.empty();
    }

    @Override
    public Class<AbilitySpawn> getInterfaceType() {
        return null;
    }
}
