package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.nyaacore.Pair;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class LootConfig extends FileConfigure {

    private final InfPlugin plugin;

    public LootConfig(InfPlugin plugin) {
        this.plugin = plugin;
    }

    @Serializable
    Map<String, ILootItem> lootItemMap = new LinkedHashMap<>();
    @Serializable(name = "map")
    Map<String, Map<String, Integer>> lootMap = new LinkedHashMap<>();

    @Override
    public void load() {
        super.load();
        LootManager.loadFromLootMap(lootItemMap, lootMap);
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
