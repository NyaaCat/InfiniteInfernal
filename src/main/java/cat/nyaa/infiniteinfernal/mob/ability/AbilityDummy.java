package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.IMob;

public class AbilityDummy implements IAbility {

    public AbilityDummy(){}

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {

    }
}
