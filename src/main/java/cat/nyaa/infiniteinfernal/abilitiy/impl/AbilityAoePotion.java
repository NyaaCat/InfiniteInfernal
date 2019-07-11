package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityTick;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.stream.Stream;

public class AbilityAoePotion extends BaseAbility implements AbilityTick {
    @Serializable
    public double chance = 0.1;
    @Serializable
    public PotionEffectType effect = PotionEffectType.SLOW;
    @Serializable
    public int duration = 10;
    @Serializable
    public int amplifier = 1;
    @Serializable
    public double radius = 25;

    @Override
    public void tick(IMob iMob) {
        if (!Utils.possibility(chance))return;
        Stream<LivingEntity> entityStream = Utils.getValidTarget(iMob, iMob.getEntity().getNearbyEntities(radius,radius,radius));
        PotionEffect effect = this.effect.createEffect(duration,amplifier);
        entityStream.forEach(livingEntity -> livingEntity.addPotionEffect(effect));
    }

    @Override
    public String getName() {
        return "AoePotion";
    }
}
