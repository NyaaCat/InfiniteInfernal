package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.utils.ClassPathUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class AbilityManager {
    private static Map<String, IAbility> prototypeMap = new HashMap<>();

    public static void initPrototypeMap(){
        prototypeMap.clear();
        Class<? extends IAbility>[] ACTIVE_CLASSES = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.mob.ability.impl.active", IAbility.class);
        Class<? extends IAbility>[] PASSIVE_CLASSES = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.mob.ability.impl.passive", IAbility.class);
        for (Class<? extends IAbility> cls : ACTIVE_CLASSES) {
            registerAbility(cls);
        }
        for (Class<? extends IAbility> cls : PASSIVE_CLASSES) {
            registerAbility(cls);
        }
    }

    private static void registerAbility(Class<? extends IAbility> cls) {
        try{
            IAbility iAbility = cls.getConstructor().newInstance();
            String name = iAbility.getName();
            prototypeMap.put(name, iAbility);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "error loading ability list: ", e);
        }
    }

    public static IAbility copyOf(String abilityName) {
        IAbility iAbility = prototypeMap.get(abilityName);
        if (iAbility == null){
            return null;
        }
        ConfigurationSection s = new YamlConfiguration();
        iAbility.serialize(s);
        try {
            IAbility iAbility1 = iAbility.getClass().getConstructor().newInstance();
            iAbility1.deserialize(s);
            return iAbility1;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
