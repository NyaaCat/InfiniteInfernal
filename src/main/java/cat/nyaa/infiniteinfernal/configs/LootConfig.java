package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LootConfig extends FileConfigure {

    private final InfPlugin plugin;

    public LootConfig(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @Serializable(name = "items")
    Map<String, ILootItem> lootItemMap = new LinkedHashMap<>();
    @Serializable(name = "map")
    Map<String, LootWeight> lootMap = new LinkedHashMap<>();

    public static class LootWeight implements ISerializable{
        @Serializable
        public Map<String, Integer> weightMap = new LinkedHashMap<>();
    }

    @Override
    public void load() {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(ensureFile());
        } catch (IOException | InvalidConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        deserialize(cfg);
        LootManager.loadFromLootMap(lootItemMap, lootMap);
    }

    private File ensureFile() {
        File cfgFile = new File(getPlugin().getDataFolder(), getFileName());
        if (!cfgFile.exists()) {
            cfgFile.getParentFile().mkdirs();
            try {
                cfgFile.createNewFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return cfgFile;
    }

    @Override
    public void deserialize(ConfigurationSection config) {
        ISerializable.deserialize(config, this);
    }

    @Override
    public void serialize(ConfigurationSection config) {
        LootManager.serializeDrops(lootItemMap, lootMap);
        ISerializable.serialize(config,this);
    }

    @Override
    protected String getFileName() {
        return "loot.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }
}
