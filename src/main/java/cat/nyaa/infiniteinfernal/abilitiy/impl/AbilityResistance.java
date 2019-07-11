package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityHurt;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.event.entity.EntityDamageEvent;

public class AbilityResistance extends BaseAbility implements AbilityHurt {

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
