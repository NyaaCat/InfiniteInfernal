package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityDeath;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.mob.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.utils.Utils;
import co.aikar.taskchain.BukkitTaskChainFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AbilityImmunity extends AbilityPassive implements AbilitySpawn, AbilityDeath {
    private static List<IMob> affected = new ArrayList<>();
    private static final String CACHE_EFFECT = "EFFECT";

    @Serializable
    public List<String> effects = new ArrayList<>(Collections.singletonList("SPEED"));

    private static boolean inited = false;

    private static Listener listener;

    private void createCache(IMob iMob, LivingEntity target) {
        if (cache == null) {
            cache = cacheBuilder.build();
        }
        Set<PotionEffectType> peT = new HashSet<>();
        UUID uniqueId = target.getUniqueId();
        List<IAbilitySet> abilities = iMob.getAbilities();
        List<AbilityImmunity> immunities = new ArrayList<>();
        if (abilities != null){
            abilities.forEach(iAbilitySet -> {
                List<AbilityImmunity> abilitiesInSet = iAbilitySet.getAbilitiesInSet(AbilityImmunity.class);
                immunities.addAll(abilitiesInSet);
            });
        }
        immunities.stream().flatMap(abilityImmunity -> abilityImmunity.effects.stream())
                .forEach(s -> {
                    PotionEffectType potionEffect = Utils.parseEffect(s, getName());
                    peT.add(potionEffect);
                });
        cache.put(uniqueId, peT);
        peT.forEach(potionEffectType -> clearEffect(target, potionEffectType));
    }

    private static CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .initialCapacity(100)
            .expireAfterAccess(60 , TimeUnit.SECONDS);

    private static Cache<UUID, Set<PotionEffectType>> cache = cacheBuilder.build();

    public void clearEffect(LivingEntity entity, PotionEffectType type) {
        entity.removePotionEffect(type);
    }

    public void init() {
        listener = new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onPotion(EntityPotionEffectEvent ev) {
                Entity entity1 = ev.getEntity();
                Set<PotionEffectType> potions = cache.getIfPresent(entity1.getUniqueId());
                IMob iMob = MobManager.instance().toIMob(entity1);
                if (iMob == null)return;
                if(!affected.contains(iMob))return;
                PotionEffect newEffect = ev.getNewEffect();
                PotionEffect oldEffect = ev.getOldEffect();
                if (potions != null && !potions.isEmpty()) {
                    if (potions.contains(ev.getModifiedType())){
                        if (newEffect == null){
                            return;
                        }
                        ev.setCancelled(true);
                    }
                } else {
                    Entity entity = ev.getEntity();
                    if (entity instanceof LivingEntity) {
                        BukkitTaskChainFactory.create(InfPlugin.plugin).newChain()
                                .delay(1)
                                .sync(() -> {
                                    createCache(iMob, (LivingEntity) entity);
                                }).execute();
                    }
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(listener, InfPlugin.plugin);
    }

    public void disable() {
        inited = false;
    }

    @Override
    public String getName() {
        return "Immunity";
    }

    @Override
    public void onSpawn(IMob iMob) {
        if (!inited) {
            init();
            inited = true;
        }
        affected.add(iMob);
        createCache(iMob, iMob.getEntity());
        new BukkitRunnable(){
            @Override
            public void run() {
                List<IMob> invalids = new ArrayList<>();
                affected.forEach(iMob1 -> {
                    if (!MobManager.instance().isIMob(iMob1.getEntity())){
                        invalids.add(iMob1);
                    }
                });
                invalids.stream().forEach(iMob1 -> affected.remove(iMob1));
            }
        }.runTaskLater(InfPlugin.plugin, 1);
    }

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        if (listener != null) {
            affected.remove(iMob);
        }
    }
}
