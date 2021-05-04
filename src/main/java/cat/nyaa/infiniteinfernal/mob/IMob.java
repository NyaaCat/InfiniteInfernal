package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.event.InfernalSpawnEvent;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityNearDeath;
import cat.nyaa.infiniteinfernal.mob.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.mob.controller.Aggro;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface IMob {
    Map<ILootItem, Integer> getLoots();
    Map<ILootItem, Integer> getSpecialLoots();
    List<IAbilitySet> getAbilities();
    LivingEntity getEntity();
    EntityType getEntityType();
    KeyedBossBar getBossBar();
    LivingEntity getTarget();

    double getDamage();
    double getDamageResist();
    double getMovementSpeed();
    double getMaxHealth();
    double getSpecialChance();
    int getExp();
    boolean isAutoSpawn();
    boolean dropVanilla();
    boolean isDynamicHealth();
    String getLevel();
    String getName();
    String getTaggedName();

    void showParticleEffect();
    void makeInfernal(LivingEntity entity);
    void autoRetarget();
    void retarget(LivingEntity entity);
    void tweakHealth();

    default <T> void triggerAbility(IMob iMob, T event, Class<?> abilityCls) {
        Class<?> evtCls = event.getClass();

        List<IAbilitySet> available = this.getAbilities().stream()
                .filter(iAbilitySet -> iAbilitySet.containsClass(abilityCls))
                .collect(Collectors.toList());
        IAbilitySet iAbilitySet = RandomUtil.weightedRandomPick(available);
        if (iAbilitySet == null){
            return;
        }

    }

    Map<LivingEntity, Aggro> getNonPlayerTargets();

    boolean isTarget(LivingEntity target);
    MobConfig getConfig();

    void onDeath();

    EntityDamageEvent getLastDamageCause();
    void setLastDamageCause(EntityDamageEvent event);

    void updateBossBar(KeyedBossBar bossBar, LivingEntity entity);
}
