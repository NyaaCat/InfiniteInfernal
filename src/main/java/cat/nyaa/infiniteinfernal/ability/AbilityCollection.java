package cat.nyaa.infiniteinfernal.ability;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.utils.ClassPathUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AbilityCollection {
    public static final Class<? extends IAbility>[] ACTIVE_CLASSES;
    public static final Class<? extends IAbility>[] PASSIVE_CLASSES;

    public static final Map<String, Class<? extends IAbility>> ABILITY_NAMES;

    static {
        ACTIVE_CLASSES = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.ability.impl.active", IAbility.class);
        PASSIVE_CLASSES = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.ability.impl.passive", IAbility.class);
        ABILITY_NAMES = new LinkedHashMap<>();
        Arrays.stream(ACTIVE_CLASSES).forEach(aClass -> ABILITY_NAMES.put(aClass.getSimpleName().replaceFirst("Ability", ""), aClass));
        Arrays.stream(PASSIVE_CLASSES).forEach(aClass -> ABILITY_NAMES.put(aClass.getSimpleName().replaceFirst("Ability", ""), aClass));
    }
}
