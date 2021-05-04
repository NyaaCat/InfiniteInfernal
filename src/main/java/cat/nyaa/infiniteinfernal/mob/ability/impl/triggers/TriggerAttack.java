package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class TriggerAttack extends Trigger<AbilityAttack, Void, EntityDamageByEntityEvent> {
    @Override
    public Optional<Void> trigger(IMob iMob, AbilityAttack ability, EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof LivingEntity){
            ability.onAttack(iMob, ((LivingEntity) entity));
        }
        return Optional.empty();
    }

    @Override
    public Class<AbilityAttack> getInterfaceType() {
        return AbilityAttack.class;
    }
}
