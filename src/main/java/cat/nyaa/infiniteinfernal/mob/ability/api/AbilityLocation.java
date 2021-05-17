package cat.nyaa.infiniteinfernal.mob.ability.api;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;

public interface AbilityLocation {
    void fire(IMob mob, MobCastEvent event);
}
