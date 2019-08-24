package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AbilityClearEffect extends AbilityPassive implements AbilityAttack {
    private static List<UUID> affected = new ArrayList<>();
    private static final String CACHE_EFFECT = "EFFECT";

    @Serializable
    public double attackChance = 0.5;
    @Serializable
    public int duration = 60;
    @Serializable
    public List<String> effects = new ArrayList<>();
    private boolean inited = false;

    private static Listener listener;
    private int durationWatcher = duration;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (!Utils.possibility(attackChance)) return;
        if (!inited) {
            init();
            inited = true;
        }
        affect(target);
    }

    private void affect(LivingEntity target) {
        if (target == null) return;
        UUID uniqueId = target.getUniqueId();
        affected.add(uniqueId);
        new BukkitRunnable() {
            @Override
            public void run() {
                affected.remove(uniqueId);
            }
        }.runTaskLater(InfPlugin.plugin, duration);
        createCacheAndClearEffect(target);
    }

    private void createCacheAndClearEffect(LivingEntity target) {
        if (duration != durationWatcher) {
            cache = CacheBuilder.newBuilder()
                    .concurrencyLevel(2)
                    .initialCapacity(100)
                    .expireAfterAccess((long) (((double) duration) / 20d), TimeUnit.SECONDS).build();
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
            peT.forEach(potionEffectType -> clearEffect(target, potionEffectType));
        }else {
            List<PotionEffectType> finalPeT1 = peT;
            class ClearTask extends BukkitRunnable{
                private LivingEntity target;

                private ClearTask(LivingEntity target){
                    this.target = target;
                }
                @Override
                public void run() {
                    if (affected.contains(target.getUniqueId())){
                        finalPeT1.forEach(potionEffectType -> clearEffect(target, potionEffectType));
                        new ClearTask(target).runTaskLater(InfPlugin.plugin, 1);
                    }
                }
            }
            new ClearTask(target).runTaskLater(InfPlugin.plugin, 1);
        }
    }

    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
            .concurrencyLevel(2)
            .initialCapacity(100)
            .expireAfterAccess((long) (((double) duration) / 20d), TimeUnit.SECONDS);

    Cache<String, List<PotionEffectType>> cache = cacheBuilder.build();

    public void clearEffect(LivingEntity entity, PotionEffectType type) {
        entity.removePotionEffect(type);
    }

    public void init() {
        listener = new Listener() {
            @EventHandler
            public void onPotion(EntityPotionEffectEvent ev) {
                List<PotionEffectType> potions = cache.getIfPresent(CACHE_EFFECT);
                if(!affected.contains(ev.getEntity().getUniqueId()))return;
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
                        createCacheAndClearEffect((LivingEntity) entity);
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
        return "ClearEffect";
    }
}
