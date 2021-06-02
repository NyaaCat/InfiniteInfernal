package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityDeath;
import cat.nyaa.infiniteinfernal.mob.ability.trigger.Trigger;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

public class TriggerDeath extends Trigger<AbilityDeath, Void, EntityDeathEvent> {
    public TriggerDeath(String name) {
        super(name);
    }

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