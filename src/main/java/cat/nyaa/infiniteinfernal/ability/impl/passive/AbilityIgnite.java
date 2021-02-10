package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.LivingEntity;

public class AbilityIgnite  extends AbilityPassive implements AbilityAttack {

    @Serializable
    public int duration = 40;
    @Serializable
    public double chance = 100d;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (Utils.possibility(chance / 100d)){
            target.setFireTicks(duration);
        }
    }

    @Override
    public String getName() {
        return "ignite";
    }
}
