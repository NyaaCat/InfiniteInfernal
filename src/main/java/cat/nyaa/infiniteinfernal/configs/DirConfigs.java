package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DirConfigs<T extends IdFileConfig> implements ISerializable {
    protected File storageDir;
    private final Class<T> targetClass;
    int id = 0;

    public DirConfigs(File storageDir, Class<T> targetClass) {
        this.storageDir = storageDir;
        this.targetClass = targetClass;
    }

    private Map<Integer, T>
            configures = new LinkedHashMap<>();

    public int add(T config) {
        while (configures.containsKey(id)) {
            id++;
        }
        configures.put(id, config);
        config.setId(id);
        return id;
    }

    public T remove(int id) {
        return configures.remove(id);
    }

    public Collection<T> values() {
        return configures.values();
    }

    public void loadFromDir() {
        this.clear();
        if (storageDir.exists()) {
            File[] files = storageDir.listFiles(pathname -> pathname.getName().endsWith(".yml"));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    try {
                        String[] split = file.getName().split("-");
                        if (split.length == 2) {
                            String s = split[1].split("\\.")[0];
                            Integer id = Integer.valueOf(s);
                            T t = targetClass.getConstructor(int.class).newInstance(id);
//                            if (t.getFileName().equals(file.getName())) {
//                                t.load();
                                configures.put(id, t);
                                YamlConfiguration config = new YamlConfiguration();
                                config.load(file);
                                t.deserialize(config);
//                            }

                            if (id > this.id) {
                                this.id = id;
                            }
                        }
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | NumberFormatException | InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (InvalidConfigurationException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void saveToDir() {
        if (!configures.isEmpty()) {
            configures.forEach((integer, t) -> t.save());
        }
    }

    public void clear(){
        configures.clear();
        id = 0;
    }

    public T parseId(String s) throws IllegalArgumentException {
        String[] split = s.split("-");
        if (split.length < 2) {
            throw new IllegalArgumentException();
        }
        int id = Integer.parseInt(split[1]);
        return this.configures.get(id);
    }

    public T get(int id) {
        return configures.get(id);
    }
}
