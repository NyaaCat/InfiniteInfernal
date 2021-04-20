package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.mob.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.event.entity.EntityDamageEvent;

public class AbilityResistance extends AbilityPassive implements AbilityHurt {

    @Serializable
    public double percentile = 0;

    @Override
    public void onHurt(IMob mob, EntityDamageEvent event) {
        double origDamage = event.getDamage();
        double resist = origDamage * (percentile / 100d);
        event.setDamage(Math.max(0, origDamage - resist));
    }

    @Override
    public String getName() {
        return "Resistance";
    }
}
