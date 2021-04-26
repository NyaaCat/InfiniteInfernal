package cat.nyaa.infiniteinfernal.utils.hook;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class HookUtil {
    /**
     * run all @Hook(hookName) methods in obj
     * @param hookName hook name, String.
     * @param obj object to be triggered
     * @param <T> type of object
     */
    public static <T> void runHook(String hookName, T obj) {
        Class<?> cls = obj.getClass();
        List<Class<?>> classList = new ArrayList<>();
        while (cls.getSuperclass() != null) {
            classList.add(cls);
            cls = cls.getSuperclass();
        }
        classList.stream().flatMap(clss -> Arrays.stream(clss.getDeclaredMethods()))
                .filter(method -> {
                    Hook annotation = method.getAnnotation(Hook.class);
                    return annotation != null && annotation.value().equals(hookName);
                })
                .sorted(Comparator.comparing(method -> method.getAnnotation(Hook.class).priority()))
                .forEach(method -> {
                    try {
                        method.invoke(obj);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });
    }
}

