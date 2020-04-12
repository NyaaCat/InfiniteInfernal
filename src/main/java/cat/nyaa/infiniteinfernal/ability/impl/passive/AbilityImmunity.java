package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityDeath;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AbilityImmunity extends AbilityPassive implements AbilitySpawn, AbilityDeath {
    private static List<IMob> affected = new ArrayList<>();
    private static final String CACHE_EFFECT = "EFFECT";

    @Serializable
    public List<String> effects = new ArrayList<>(Collections.singletonList("SPEED"));

    private static boolean inited = false;

    private static Listener listener;

    private void createCacheAndClearEffect(LivingEntity target) {
        if (cache == null) {
            cache = cacheBuilder.build();
        }
        List<PotionEffectType> peT;
        peT = cache.getIfPresent(CACHE_EFFECT);
        if (peT == null) {
            peT = new ArrayList<>(effects.size());
        }
        if (peT.isEmpty()) {
            if (!effects.isEmpty()) {
                List<PotionEffectType> finalPeT = peT;
                effects.forEach(s -> {
                    PotionEffectType potionEffect = Utils.parseEffect(s, getName());
                    finalPeT.add(potionEffect);
                    clearEffect(target, potionEffect);
                });
            }
            cache.put(CACHE_EFFECT, peT);
        }
        peT.forEach(potionEffectType -> clearEffect(target, potionEffectType));
    }

    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .concurrencyLevel(2)
            .initialCapacity(100)
            .expireAfterAccess(60 , TimeUnit.SECONDS);

    Cache<String, List<PotionEffectType>> cache = cacheBuilder.build();

    public void clearEffect(LivingEntity entity, PotionEffectType type) {
        entity.removePotionEffect(type);
    }

    public void init() {
        listener = new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onPotion(EntityPotionEffectEvent ev) {
                List<PotionEffectType> potions = cache.getIfPresent(CACHE_EFFECT);
                IMob iMob = MobManager.instance().toIMob(ev.getEntity());
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
                                    createCacheAndClearEffect((LivingEntity) entity);
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
        createCacheAndClearEffect(iMob.getEntity());
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
