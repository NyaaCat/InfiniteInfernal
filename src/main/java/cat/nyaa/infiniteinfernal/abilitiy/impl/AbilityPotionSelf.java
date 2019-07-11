package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityTick;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.potion.PotionEffectType;

public class AbilityPotionSelf extends BaseAbility implements AbilityTick {
    @Serializable
    public PotionEffectType effect = PotionEffectType.SPEED;
    @Serializable
    public int duration = 1;
    @Serializable
    public int amplifier = 1;

    @Override
    public void tick(IMob iMob) {
        iMob.getEntity().addPotionEffect(effect.createEffect(duration,amplifier));
    }

    @Override
    public String getName() {
        return "PotionSelf";
    }
}
