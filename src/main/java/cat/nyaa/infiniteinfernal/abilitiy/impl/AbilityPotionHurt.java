package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityHurt;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

public class AbilityPotionHurt extends BaseAbility implements AbilityHurt {
    @Serializable
    public PotionEffectType effect = PotionEffectType.HARM;
    @Serializable
    public int duration = 1;
    @Serializable
    public int amplifier = 1;

    @Override
    public void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity){
            ((LivingEntity) damager).addPotionEffect(effect.createEffect(duration, amplifier));
        }
    }

    @Override
    public String getName() {
        return "PotionHurt";
    }
}
