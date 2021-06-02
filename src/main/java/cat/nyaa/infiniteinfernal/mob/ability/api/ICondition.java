package cat.nyaa.infiniteinfernal.mob.ability.api;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.InvocationTargetException;

public interface ICondition extends ISerializable {
    String getId();
    boolean check(IMob mob);

    static ICondition copyOf(ICondition iCondition){
        Class<? extends ICondition> aClass = iCondition.getClass();
        try {
            ICondition iCondition1 = aClass.getConstructor().newInstance();
            YamlConfiguration cfg = new YamlConfiguration();
            iCondition.serialize(cfg);
            iCondition1.deserialize(cfg);
            return iCondition1;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("no constructor found, please contact admin.", e);
        }
    }
}
