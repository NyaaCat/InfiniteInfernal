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
    public static final Trigger<AbilityActive, Void, MobTickEvent> ACTIVE = new TriggerActive();
    public static final Trigger<AbilityAttack, Void, EntityDamageByEntityEvent> ATTACK = new TriggerAttack();
    public static final Trigger<AbilityDeath, Void, EntityDeathEvent> DEATH = new TriggerDeath();
    public static final Trigger<AbilityHurt, Void, EntityDamageEvent> HURT = new TriggerHurt();
    public static final Trigger<AbilityLocation, Void, MobCastEvent> LOCATION = new TriggerLocation();
    public static final Trigger<AbilitySpawn, Void, MobSpawnEvent> SPAWN = new TriggerSpawn();
    public static final Trigger<AbilityNearDeath, Void, MobNearDeathEvent> NEARDEATH = new TriggerNeardeath();

    public static Map<String, Trigger<?,?,?>> triggerMap = new HashMap<>();

    private static final List<String> protectedTriggers = new ArrayList<>();

    public static void clear(){
        triggerMap.clear();
        protectedTriggers.clear();
    }

    public static void registerInternalTriggerMap(){
        triggerMap.put("ACTIVE", ACTIVE);
        triggerMap.put("ATTACK", ATTACK);
        triggerMap.put("DEATH", DEATH);
        triggerMap.put("HURT", HURT);
        triggerMap.put("LOCATION", LOCATION);
        triggerMap.put("SPAWN", SPAWN);
        triggerMap.put("NEARDEATH", NEARDEATH);
        protectedTriggers.add("ACTIVE");
        protectedTriggers.add("ATTACK");
        protectedTriggers.add("DEATH");
        protectedTriggers.add("HURT");
        protectedTriggers.add("LOCATION");
        protectedTriggers.add("SPAWN");
        protectedTriggers.add("NEARDEATH");
    }

    public static Trigger<?, ?, ?> getTrigger(String triggerName) {
        return triggerMap.get(triggerName);
    }
}
