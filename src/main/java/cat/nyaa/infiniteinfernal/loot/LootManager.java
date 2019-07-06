package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.InfPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class LootManager {
    private static LootManager instance;
    private final InfPlugin plugin;
    private Map<Integer, List<ILootItem>> commonDrops = new LinkedHashMap<>();
    private Map<String, ILootItem> lootItemMap = new LinkedHashMap<>();
    private Map<ILootItem, Map<Integer, Integer>> itemWeightMap = new LinkedHashMap<>();

    private LootManager(InfPlugin plugin) {
        this.plugin = plugin;
    }

    public static LootManager instance() {
        if (instance == null) {
            synchronized (LootManager.class) {
                if (instance == null) {
                    instance = new LootManager(InfPlugin.plugin);
                }
            }
        }
        return instance;
    }

    public static void disable() {
        instance = null;
    }

    public List<ILootItem> getLevelDrops(int level) {
        return commonDrops.get(level);
    }

    public void addLoot(String name, boolean dynamic, ItemStack itemStack){
        ILootItem lootItem;
        if (itemStack.getType().equals(Material.ENCHANTED_BOOK)){
            lootItem = new EnchantmentBookLootItem(plugin, name, itemStack);
        }else {
            lootItem = new CommonLootItem(plugin, name, itemStack);
        }
        lootItem.setDynamic(dynamic);
        lootItemMap.put(name, lootItem);
    }

    public ILootItem getLoot(String name){
        return lootItemMap.get(name);
    }

    public void setDrop(String item, int level, int weight){
        ILootItem lootItem = lootItemMap.get(item);
        if (lootItem == null){
            throw new RuntimeException();
        }
        List<ILootItem> iLootItems = commonDrops.computeIfAbsent(level, integer -> new ArrayList<>());
        iLootItems.add(lootItem);
        Map<Integer, Integer> weightMap = itemWeightMap.computeIfAbsent(lootItem, iLootItem -> new LinkedHashMap<>());
        weightMap.put(level, weight);
    }

    public List<ILootItem> inspect(int level){
        return commonDrops.get(level);
    }

    public static int getWeightForLevel(ILootItem lootItem, int level) {
        Map<ILootItem, Map<Integer, Integer>> itemWeightMap = instance.itemWeightMap;
        if (itemWeightMap.containsKey(lootItem)) {
            Map<Integer, Integer> levelWeightMap = itemWeightMap.get(lootItem);
            return levelWeightMap.getOrDefault(level, -1);
        } else {
            return -1;
        }
    }

    public static void serializeDrops(Map<String, ILootItem> lootItemMap, Map<String, Map<String, Integer>> lootMap) {
        if (!instance.lootItemMap.isEmpty()){
            lootItemMap.clear();
            lootItemMap.putAll(instance.lootItemMap);
        }
        if (!instance.commonDrops.isEmpty()) {
            lootMap.clear();
            instance.commonDrops.forEach((level, lootItems) -> {
                String levelStr = "level-" + level;
                Map<String, Integer> levelLootMap = new LinkedHashMap<>(lootItems.size());
                lootItems.forEach(iLootItem -> {
                    levelLootMap.put(iLootItem.getName(), getWeightForLevel(iLootItem, level));
                });
                lootMap.put(levelStr, levelLootMap);
            });
        }
    }

    public static void loadFromLootMap(Map<String, ILootItem> lootItemMap, Map<String, Map<String, Integer>> levels) {
        instance.lootItemMap = lootItemMap;
        if (!levels.isEmpty()) {
            levels.forEach((levelStr, iLootItems) -> {
                if (!levelStr.startsWith("level-")) {
                    return;
                }
                String[] split = levelStr.split("-");
                if (split.length != 2) {
                    instance.plugin.getLogger().log(Level.WARNING, "wrong loot.map.level \"" + levelStr + "\", skipping");
                    return;
                }
                try {
                    Integer level = Integer.valueOf(split[1]);
                    List<ILootItem> items = new ArrayList<>(iLootItems.size());
                    iLootItems.forEach((s, weight) -> {
                        ILootItem iLootItem = instance.lootItemMap.get(s);
                        if (iLootItem == null) {
                            instance.plugin.getLogger().log(Level.WARNING, "no item \"" + s + "\" found, skipping");
                            return;
                        }
                        Map<Integer, Integer> levelWeight = instance.itemWeightMap.computeIfAbsent(iLootItem, iLootItem1 -> new LinkedHashMap<>());
                        levelWeight.put(level, weight);
                        items.add(iLootItem);
                    });
                    instance.commonDrops.put(level, items);
                } catch (NumberFormatException ex) {
                    instance.plugin.getLogger().log(Level.WARNING, "wrong loot.map.level \"" + levelStr + "\", skipping");
                }

            });
        }
    }
}
