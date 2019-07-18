package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;

public class AbilityFire extends ActiveAbility {
    @Serializable
    public int duration = 60;

    @Override
    public void active(IMob iMob) {
        Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(25, 25, 25))
                .forEach(entity -> entity.setFireTicks(duration));
    }

    @Override
    public String getName() {
        return "Fire";
    }
}
