package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.mob.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.context.Context;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
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
        if (damager instanceof Projectile){
            damager = (Entity) ((Projectile) damager).getShooter();
            if (damager == null)return;
        }
        if (damager instanceof LivingEntity) {
            Context.instance().putTemp(damager.getUniqueId(), DAMAGE_THORN, true);
            ((LivingEntity) damager).damage(getThornDamage(iMob, ev), iMob.getEntity());
            damager.getWorld().playSound(damager.getLocation(), Sound.ENCHANT_THORNS_HIT, 1,1);
        }
    }

    private double getThornDamage(IMob iMob, EntityDamageEvent event) {
        return Math.min(event.getFinalDamage() * (percentile / 100d), iMob.getDamage());
    }

    @Override
    public String getName() {
        return "Thorn";
    }
}
