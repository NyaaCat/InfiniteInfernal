package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.event.MobNearDeathEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;

public interface AbilityNearDeath {
    void onDeath(IMob iMob, MobNearDeathEvent mobNearDeathEvent);
}
