package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.ability.impl.triggers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Triggers {
    public static final String ACTIVE = "ACTIVE";
    public static final String ATTACK = "ATTACK";
    public static final String DEATH = "DEATH";
    public static final String HURT = "HURT";
    public static final String LOCATION = "LOCATION";
    public static final String NEARDEATH = "NEARDEATH";

    public static Map<String, Trigger<?,?,?>> triggerMap = new HashMap<>();

    private static final List<String> protectedTriggers = new ArrayList<>();

    public static void clear(){
        triggerMap.clear();
        protectedTriggers.clear();
    }

    public static void registerInternalTriggerMap(){
        triggerMap.put(ACTIVE, new TriggerActive());
        triggerMap.put(ATTACK, new TriggerAttack());
        triggerMap.put(DEATH, new TriggerDeath());
        triggerMap.put(HURT, new TriggerHurt());
        triggerMap.put(LOCATION, new TriggerLocation());
        triggerMap.put(NEARDEATH, new TriggerNeardeath());
        protectedTriggers.add(ACTIVE);
        protectedTriggers.add(ATTACK);
        protectedTriggers.add(DEATH);
        protectedTriggers.add(HURT);
        protectedTriggers.add(LOCATION);
        protectedTriggers.add(NEARDEATH);
    }

    public static Trigger<?, ?, ?> getTrigger(String triggerName) {
        return triggerMap.get(triggerName);
    }
}
