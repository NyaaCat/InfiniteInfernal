package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

public class AbilityPotionHit extends BaseAbility implements AbilityAttack {
    @Serializable
    public PotionEffectType effect = PotionEffectType.HARM;
    @Serializable
    public int duration = 1;
    @Serializable
    public int amplifier = 1;

    @Override
    public String getName() {
        return "PotionHit";
    }

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        target.addPotionEffect(effect.createEffect(duration,amplifier));
    }
}
