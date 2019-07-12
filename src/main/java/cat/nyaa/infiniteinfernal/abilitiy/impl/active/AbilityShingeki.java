package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;

public class AbilityShingeki extends ActiveAbility {
    @Serializable
    public int amount = 5;

    @Serializable
    public int delay = 10;

    @Serializable
    public double damageAmplifier = 2.0;
    
    @Override
    public void active(IMob iMob) {
        Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(10, 10, 10))
                .forEach(entity -> {
                    for (int i = 0; i < amount; i++) {
                        entity.getWorld().strikeLightningEffect(entity.getLocation());
                        entity.damage(damageAmplifier * iMob.getDamage(), iMob.getEntity());
                    }
                });
    }

    @Override
    public String getName() {
        return "Shingeki";
    }
}
