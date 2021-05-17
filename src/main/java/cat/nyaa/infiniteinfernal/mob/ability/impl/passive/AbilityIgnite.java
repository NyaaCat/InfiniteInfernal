package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import org.bukkit.entity.LivingEntity;

public class AbilityIgnite  extends AbilityPassive implements AbilityAttack {

    @Serializable
    public int duration = 40;
    @Serializable
    public double chance = 1d;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (RandomUtil.possibility(chance)){
            target.setFireTicks(duration);
        }
    }

    @Override
    public String getName() {
        return "ignite";
    }
}
