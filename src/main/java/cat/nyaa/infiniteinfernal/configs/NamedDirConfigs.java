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

public class NamedDirConfigs<T extends NamedFileConfig> implements ISerializable {
    private Map<String, T> configs = new LinkedHashMap<>();
    protected File storageDir;
    private final Class<T> targetClass;

    public NamedDirConfigs(File storageDir, Class<T> targetClass) {
        this.storageDir = storageDir;
        this.targetClass = targetClass;
    }

    public String add(T config) {
        String key = String.valueOf(configs.size());
        configs.put(key, config);
        return key;
    }

    public String add(String name, T config) {
        if (configs.containsKey(name)) {
            throw new IllegalArgumentException(name.concat(" exists"));
        }
        configs.put(name, config);
        return name;
    }

    public T remove(String id) {
        return configs.remove(id);
    }

    public Collection<T> values() {
        return configs.values();
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
                            String name = split[1].split("\\.")[0];

                            T t = targetClass.getConstructor(String.class).newInstance(name);
//                            if (t.getFileName().equals(file.getName())) {
//                                t.load();
                            configs.put(name, t);
                            YamlConfiguration config = new YamlConfiguration();
                            config.load(file);
                            t.deserialize(config);
//                            }
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
        if (!configs.isEmpty()) {
            configs.forEach((integer, t) -> t.save());
        }
    }

    public void clear() {
        configs.clear();
    }

    public T parseName(String s) throws IllegalArgumentException {
        String[] split = s.split("-");
        if (split.length < 2) {
            throw new IllegalArgumentException();
        }
        return this.configs.get(split[1]);
    }

    public T get(int id) {
        return configs.get(id);
    }
}
