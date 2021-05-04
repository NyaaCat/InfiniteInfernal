package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;

public interface AbilityLocation {
    void fire(IMob mob, MobCastEvent event);
}
