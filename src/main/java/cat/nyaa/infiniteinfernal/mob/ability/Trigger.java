package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;

import java.util.Optional;

public abstract class Trigger<T, R, Evt> {
    public abstract Optional<R> trigger(IMob iMob, T ability, Evt event);

    public abstract Class<T> getInterfaceType();
}
