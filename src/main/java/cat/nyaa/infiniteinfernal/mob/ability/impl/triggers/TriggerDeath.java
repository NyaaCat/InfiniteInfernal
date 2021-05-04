package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityDeath;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

public class TriggerDeath extends Trigger<AbilityDeath, Void, EntityDeathEvent> {
    @Override
    public Optional<Void> trigger(IMob iMob, AbilityDeath ability, EntityDeathEvent event) {
        ability.onMobDeath(iMob, event);
        return Optional.empty();
    }

    @Override
    public Class<AbilityDeath> getInterfaceType() {
        return AbilityDeath.class;
    }
}
