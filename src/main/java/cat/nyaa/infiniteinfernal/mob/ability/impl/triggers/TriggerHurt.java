package cat.nyaa.infiniteinfernal.mob.ability.impl.triggers;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Optional;

public class TriggerHurt extends Trigger<AbilityHurt, Void, EntityDamageEvent> {
    @Override
    public Optional<Void> trigger(IMob iMob, AbilityHurt ability, EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent){
            final EntityDamageByEntityEvent castedEvt = (EntityDamageByEntityEvent) event;
            Entity evtDamager = castedEvt.getDamager();
            LivingEntity damager;
            if (evtDamager instanceof Projectile) {
                ProjectileSource source = ((Projectile) evtDamager).getShooter();
                if (!(source instanceof Player)){
                    ability.onHurtByNonPlayer(iMob, castedEvt);
                }else {
                    ability.onHurtByPlayer(iMob, castedEvt);
                }
            }else if (evtDamager instanceof Player){
                ability.onHurtByPlayer(iMob, castedEvt);
            }else {
                ability.onHurtByNonPlayer(iMob, castedEvt);
            }
        }else {
            ability.onHurt(iMob, event);
        }
        return Optional.empty();
    }

    @Override
    public Class<AbilityHurt> getInterfaceType() {
        return AbilityHurt.class;
    }
}
