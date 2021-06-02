package cat.nyaa.infiniteinfernal.mob.ability.condition;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.api.ICondition;
import cat.nyaa.nyaacore.utils.ClassPathUtils;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConditionManager {
    private static Map<String, ICondition> prototypeMap = new HashMap<>();

    public static void initPrototypeMap(){
        prototypeMap.clear();
        Class<? extends ICondition>[] CONDITIONS = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.mob.ability.impl.condition", ICondition.class);
        for (Class<? extends ICondition> cls : CONDITIONS) {
            registerCondition(cls);
        }
    }
    private static void registerCondition(Class<? extends ICondition> cls) {
        try{
            ICondition iAbility = cls.getConstructor().newInstance();
            String name = iAbility.getClass().getName();
            prototypeMap.put(name, iAbility);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "error loading ability list: ", e);
        }
    }

    public static ICondition copyOf(String conditionName) {
        ICondition condition = prototypeMap.get(conditionName);
        if (condition == null){
            return null;
        }
       return ICondition.copyOf(condition);
    }
}
