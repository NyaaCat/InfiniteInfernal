package cat.nyaa.infiniteinfernal.abilitiy.impl.passive;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityAttack;
import cat.nyaa.infiniteinfernal.abilitiy.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.LivingEntity;

public class AbilityPotionHit extends AbilityPassive implements AbilityAttack {
    @Serializable
    public String effect = "HARM";
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
        Utils.doEffect(effect, target, duration, amplifier, getName());
    }
}
