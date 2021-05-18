package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityHurt;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class AbilityPotionHurt extends AbilityPassive implements AbilityHurt {
    @Serializable
    public String effect = "HARM";
    @Serializable
    public int duration = 1;
    @Serializable
    public int amplifier = 1;

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity){
            Utils.doEffect(effect, (LivingEntity) damager, duration, amplifier, getName());
        }
    }

    @Override
    public String getName() {
        return "PotionHurt";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        event.getSelectedEntities().forEach(livingEntity -> {
            onHurtByPlayer(mob, new EntityDamageByEntityEvent(mob.getEntity(), livingEntity, EntityDamageEvent.DamageCause.CUSTOM,1));
        });
    }
}
