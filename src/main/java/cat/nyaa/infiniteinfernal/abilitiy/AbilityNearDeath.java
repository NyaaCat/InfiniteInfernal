package cat.nyaa.infiniteinfernal.abilitiy;

import cat.nyaa.infiniteinfernal.event.IMobNearDeathEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;

public interface AbilityNearDeath {
    void onDeath(IMob iMob, IMobNearDeathEvent mobNearDeathEvent);
}
