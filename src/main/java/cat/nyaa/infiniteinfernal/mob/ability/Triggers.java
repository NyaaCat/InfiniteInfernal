package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.event.MobNearDeathEvent;
import cat.nyaa.infiniteinfernal.event.MobSpawnEvent;
import cat.nyaa.infiniteinfernal.event.MobTickEvent;
import cat.nyaa.infiniteinfernal.mob.ability.impl.triggers.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Triggers {
    public static final Trigger<AbilityActive, Void, MobTickEvent> ACTIVE = new TriggerActive("ACTIVE");
    public static final Trigger<AbilityAttack, Void, EntityDamageByEntityEvent> ATTACK = new TriggerAttack("ATTACK");
    public static final Trigger<AbilityDeath, Void, EntityDeathEvent> DEATH = new TriggerDeath("DEATH");
    public static final Trigger<AbilityHurt, Void, EntityDamageEvent> HURT = new TriggerHurt("HURT");
    public static final Trigger<AbilityLocation, Void, MobCastEvent> LOCATION = new TriggerLocation("LOCATION");
    public static final Trigger<AbilitySpawn, Void, MobSpawnEvent> SPAWN = new TriggerSpawn("SPAWN");
    public static final Trigger<AbilityNearDeath, Void, MobNearDeathEvent> NEARDEATH = new TriggerNeardeath("NEARDEATH");

    public static Map<String, Trigger<?,?,?>> triggerMap = new HashMap<>();

    private static final List<String> protectedTriggers = new ArrayList<>();

    public static void clear(){
        triggerMap.clear();
        protectedTriggers.clear();
    }

    public static void registerInternalTriggerMap(){
        triggerMap.put(ACTIVE.getName(), ACTIVE);
        triggerMap.put(ATTACK.getName(), ATTACK);
        triggerMap.put(DEATH.getName(), DEATH);
        triggerMap.put(HURT.getName(), HURT);
        triggerMap.put(LOCATION.getName(), LOCATION);
        triggerMap.put(SPAWN.getName(), SPAWN);
        triggerMap.put(NEARDEATH.getName(), NEARDEATH);
        protectedTriggers.add(ACTIVE.getName());
        protectedTriggers.add(ATTACK.getName());
        protectedTriggers.add(DEATH.getName());
        protectedTriggers.add(HURT.getName());
        protectedTriggers.add(LOCATION.getName());
        protectedTriggers.add(SPAWN.getName());
        protectedTriggers.add(NEARDEATH.getName());
    }

    public static Trigger<?, ?, ?> getTrigger(String triggerName) {
        return triggerMap.get(triggerName);
    }
}
