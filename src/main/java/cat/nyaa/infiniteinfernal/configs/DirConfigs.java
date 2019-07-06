package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.configuration.ISerializable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DirConfigs<T extends IdFileConfig> implements ISerializable {
    protected File storageDir;
    private final Class<T> targetClass;
    int id = 0;

    public DirConfigs(File storageDir, Class<T> targetClass){
        this.storageDir = storageDir;
        this.targetClass = targetClass;
    }

    private Map<Integer, T> configures = new LinkedHashMap<>();

    public int add(T config){
        while (configures.containsKey(id)){
            id++;
        }
        configures.put(id, config);
        return id;
    }

    public T remove(int id){
        return configures.remove(id);
    }

    public void loadFromDir(){
        if (storageDir.exists()){
            File[] files = storageDir.listFiles(pathname -> pathname.getName().endsWith(".yml"));
            if (files != null && files.length>0) {
                for (File file : files) {
                    try {
                        String[] split = file.getName().split("-");
                        if (split.length==2){
                            Integer id = Integer.valueOf(split[1]);
                            T t = targetClass.getConstructor(Integer.class).newInstance(id);
                            if (t.getFileName().equals(file.getName())) {
                                t.load();
                            }
                            configures.put(id, t);
                            if (id > this.id){
                                this.id = id;
                            }
                        }
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | NumberFormatException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void saveToDir(){
        if (!configures.isEmpty()) {
            configures.forEach((integer, t) -> t.save());
        }
    }
}
