package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
        T remove = configs.remove(id);
        removeFromDir(remove);
        return remove;
    }

    private void removeFromDir(T removed) {
        if (removed == null)return;
        if (storageDir.exists()) {
            File cfgFile = removed.getFile();
            if (cfgFile.exists()){
                if (!cfgFile.delete()) {
                    Bukkit.getLogger().log(Level.SEVERE, "can't remove file "+cfgFile.getPath());
                }
            }
        }
    }

    public Set<String> keys() {
        return configs.keySet();
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
                        String name = file.getName().split("\\.")[0];

                        T t = targetClass.getConstructor(String.class).newInstance(name);
//                            if (t.getFileName().equals(file.getName())) {
//                                t.load();
                        YamlConfiguration config = new YamlConfiguration();
                        config.load(file);
                        t.deserialize(config);
                        t.setFile(file);
                        configs.put(name, t);
//                            }
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

    public T get(String id) {
        return configs.get(id);
    }

}
