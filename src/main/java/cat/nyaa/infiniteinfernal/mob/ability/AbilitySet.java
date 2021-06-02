package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.api.ICondition;
import cat.nyaa.infiniteinfernal.mob.ability.trigger.ActiveMode;
import cat.nyaa.infiniteinfernal.mob.ability.trigger.Trigger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class AbilitySet implements IAbilitySet {
    private final List<IAbility> abilities;
    private int weight;
    public List<String> trigger;
    public ActiveMode activeMode;
    public Map<String, ICondition> conditions;
    //todo add possibility and cooldown

    public AbilitySet(AbilitySetConfig config){
        weight = config.weight;
        abilities = new ArrayList<>();
        config.abilities.values().stream().forEach(iAbility -> {
            try {
                IAbility clone= iAbility.getClass().getConstructor().newInstance();
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                iAbility.serialize(yamlConfiguration);
                clone.deserialize(yamlConfiguration);
                abilities.add(clone);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        this.trigger = new ArrayList<>(Arrays.asList(config.trigger.split(",")));
        this.activeMode = config.activeMode;
        this.conditions = new HashMap<>();
        config.conditions.values().forEach(cond -> {
            conditions.put(cond.getId(), cond);
        });
    }

    @Override
    public List<IAbility> getAbilitiesInSet() {
        return abilities;
    }

    @Override
    public <T> List<T> getAbilitiesInSet(Class<T> abilityClass) {
        return abilities.stream().filter(iAbility -> abilityClass.isAssignableFrom(iAbility.getClass()))
                .map(iAbility -> ((T) iAbility)).collect(Collectors.toList());
    }

    @Override
    public boolean checkConditions(IMob iMob) {
        return conditions.values().stream().allMatch(condition -> condition.check(iMob));
    }

    @Override
    public boolean containsClass(Class<?> cls) {
        return abilities.stream().anyMatch(iAbility -> cls.isAssignableFrom(iAbility.getClass()));
    }

    @Override
    public boolean hasTrigger(Trigger trigger) {
        return false;
    }

    @Override
    public <R, T, Evt extends Event> void trigger(IMob iMob, Class<?> abilityCls, Trigger<T, R, Evt> trigger, Evt event) {
        Iterator<T> iterator = this.getAbilitiesInSet().stream().filter(iAbility -> abilityCls.isAssignableFrom(iAbility.getClass()))
                .map(iAbility -> ((T) iAbility))
                .iterator();

        runAbilities(iMob, trigger, event, iterator);
    }

    private <T, R, Evt extends Event> void runAbilities(IMob iMob, Trigger<T, R, Evt> trigger, Evt event, Iterator<T> iterator) {
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
                            runAbilities(iMob, trigger, event, iterator);
                        }
                    }.runTaskLater(InfPlugin.plugin, flowCtl.getFlowCtlDelay());
                    return;
                }

            }
            trigger.trigger(iMob, next, event);
        }
    }

    @Override
    public ICondition getCondition(String condition) {
        return conditions.get(condition);
    }

    @Override
    public int getWeight() {
        return weight;
    }
}
