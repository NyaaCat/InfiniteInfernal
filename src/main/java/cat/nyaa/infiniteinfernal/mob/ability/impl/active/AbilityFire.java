package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
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

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        event.getSelectedEntities().forEach(livingEntity -> livingEntity.setFireTicks(duration));
    }
}
