package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.mob.ability.FlowCtlAbility;
import cat.nyaa.infiniteinfernal.mob.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.mob.ability.Trigger;
import cat.nyaa.infiniteinfernal.mob.controller.Aggro;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
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

    default <T, R, Evt extends Event> void triggerAbility(Trigger<T, R, Evt> trigger, Evt event) {
        Class<?> abilityCls = trigger.getInterfaceType();

        List<IAbilitySet> available = this.getAbilities().stream()
                .filter(iAbilitySet -> iAbilitySet.containsClass(abilityCls))
                .collect(Collectors.toList());
        IAbilitySet iAbilitySet = RandomUtil.weightedRandomPick(available);
        if (iAbilitySet == null){
            return;
        }
        Iterator<T> iterator = iAbilitySet.getAbilitiesInSet().stream().filter(iAbility -> abilityCls.isAssignableFrom(iAbility.getClass()))
                .map(iAbility -> ((T) iAbility))
                .iterator();

        runAbilities(trigger, event, iterator);
    }

    default <T, R, Evt extends Event> void runAbilities(Trigger<T, R, Evt> trigger, Evt event, Iterator<T> iterator) {
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (next instanceof FlowCtlAbility){
                FlowCtlAbility flowCtl = (FlowCtlAbility) next;
                if (flowCtl.aborted()){
                    return;
                }
                if (flowCtl.getFlowCtlDelay() > 0) {
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            runAbilities(trigger, event, iterator);
                        }
                    }.runTaskLater(InfPlugin.plugin, flowCtl.getFlowCtlDelay());
                    return;
                }

            }
            trigger.trigger(this, next, event);
        }
    }

    default <T, R, Evt extends Event> void triggerAllAbility(Trigger<T, R, Evt> trigger, Evt event){
        Class<?> abilityCls = trigger.getInterfaceType();

        //todo filter by trigger, not class
        List<IAbilitySet> available = this.getAbilities().stream()
                .filter(iAbilitySet -> iAbilitySet.containsClass(abilityCls))
                .collect(Collectors.toList());
        available.stream().forEach(iAbilitySet -> {
            iAbilitySet.getAbilitiesInSet().stream().filter(iAbility -> abilityCls.isAssignableFrom(iAbility.getClass()))
                    .map(iAbility -> ((T) iAbility))
                    .forEach(iAbility -> trigger.trigger(this, iAbility, event));
        });
    }

    Map<LivingEntity, Aggro> getNonPlayerTargets();

    boolean isTarget(LivingEntity target);
    MobConfig getConfig();

    void onDeath();

    EntityDamageEvent getLastDamageCause();
    void setLastDamageCause(EntityDamageEvent event);

    void updateBossBar(KeyedBossBar bossBar, LivingEntity entity);
}
