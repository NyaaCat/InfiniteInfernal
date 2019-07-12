package cat.nyaa.infiniteinfernal.abilitiy.impl.passive;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityHurt;
import cat.nyaa.infiniteinfernal.abilitiy.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class AbilityThorns extends AbilityPassive implements AbilityHurt {
    @Serializable
    public double percentile = 10;

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity){
            ((LivingEntity) damager).damage(getThornDamage(event), mob.getEntity());
        }
    }

    @Override
    public void onHurtByNonPlayer(IMob mob, EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent){
            EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) event;
            Entity damager = ev.getDamager();
            if (damager instanceof LivingEntity) {
                ((LivingEntity) damager).damage(getThornDamage(event), mob.getEntity());
            }
        }
    }

    private double getThornDamage(EntityDamageEvent event) {
        return event.getFinalDamage() * (percentile / 100d);
    }

    @Override
    public String getName() {
        return "Thorn";
    }
}
