package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Context;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class AbilityThorns extends AbilityPassive implements AbilityHurt {
    private static final String DAMAGE_THORN = "Damage Thorn";
    @Serializable
    public double percentile = 10;

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        Boolean aBoolean = Context.instance().getBoolean(mob.getEntity().getUniqueId(), DAMAGE_THORN);
        if (aBoolean != null && aBoolean) {
            return;
        }
        doThorn(mob, event);
    }

    @Override
    public void onHurtByNonPlayer(IMob mob, EntityDamageByEntityEvent event) {
        Boolean aBoolean = Context.instance().getBoolean(mob.getEntity().getUniqueId(), DAMAGE_THORN);
        if (aBoolean != null && aBoolean) {
            return;
        }
        if (event != null) {
            doThorn(mob, event);
        }
    }

    private void doThorn(IMob iMob, EntityDamageByEntityEvent ev){
        Entity damager = ev.getDamager();
        if (damager instanceof LivingEntity) {
            Context.instance().putTemp(damager.getUniqueId(), DAMAGE_THORN, true);
            ((LivingEntity) damager).damage(getThornDamage(ev), iMob.getEntity());
        }
        damager.getWorld().playSound(damager.getLocation(), Sound.ENCHANT_THORNS_HIT, 1,1);
    }

    private double getThornDamage(EntityDamageEvent event) {
        return event.getFinalDamage() * (percentile / 100d);
    }

    @Override
    public String getName() {
        return "Thorn";
    }
}
