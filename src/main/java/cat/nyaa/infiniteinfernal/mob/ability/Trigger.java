package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public abstract class Trigger<T, R, Evt> {
    static Map<String, Trigger<?, ?, ?>> triggerMap = new HashMap<>();

    public static Trigger<?, ?, ?> getTrigger(String string){
        return triggerMap.get(string);
    }

    public abstract Optional<R> trigger(IMob iMob, T ability, Evt event);
}
