package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;

import java.util.Optional;

public abstract class Trigger<AbilityImpl, R, Evt> {
    private String name;

    public Trigger(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Optional<R> trigger(IMob iMob, AbilityImpl ability, Evt event);

    public abstract Class<AbilityImpl> getInterfaceType();
}
