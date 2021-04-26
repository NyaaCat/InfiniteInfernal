package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.IllegalConfigException;
import cat.nyaa.infiniteinfernal.configs.LootConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import cat.nyaa.infiniteinfernal.utils.correction.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.correction.ICorrector;
import cat.nyaa.infiniteinfernal.utils.Utils;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

public class LootManager {
    private static LootManager instance;
    private LootConfig lootConfig;
    private final InfPlugin plugin;
    private Map<String, List<ILootItem>> commonDrops = new LinkedHashMap<>();
    private Map<String, ILootItem> lootItemMap = new LinkedHashMap<>();
    private Map<ILootItem, Map<String, Integer>> itemWeightMap = new LinkedHashMap<>();

    private LootManager(InfPlugin plugin) {
        this.plugin = plugin;
        lootConfig = new LootConfig(plugin);
    }

    public static LootManager instance() {
        if (instance == null) {
            synchronized (LootManager.class) {
                if (instance == null) {
                    instance = new LootManager(InfPlugin.plugin);
                    instance.load();
                }
            }
        }
        return instance;
    }

    public static void disable() {
        if (instance != null) {
            instance.save();
        }
        instance = null;
    }

    public void addCommonLoot(ILootItem lootItem, String level, int weight) {
        List<ILootItem> lootItems = commonDrops.computeIfAbsent(level, integer -> new ArrayList<>());
        Map<String, Integer> levelWeightMap = itemWeightMap.computeIfAbsent(lootItem, integer -> new HashMap<>());
        lootItems.add(lootItem);
        levelWeightMap.put(level, weight);
    }

    public List<ILootItem> getLevelDrops(String level) {
        return commonDrops.computeIfAbsent(level, integer -> new ArrayList<>());
    }

    public void addLoot(String name, boolean dynamic, ItemStack itemStack) {
        ILootItem lootItem;
        if (itemStack.getType().equals(Material.ENCHANTED_BOOK)) {
            lootItem = new EnchantmentBookLootItem(plugin, name, itemStack);
        } else {
            lootItem = new CommonLootItem(plugin, name, itemStack);
        }
        lootItem.setDynamic(dynamic);
        lootItemMap.put(name, lootItem);
        save();
    }

    public ILootItem getLoot(String name) {
        return lootItemMap.get(name);
    }

    public void setDrop(String item, String level, int weight) {
        ILootItem lootItem = lootItemMap.get(item);
        if (lootItem == null) {
            throw new RuntimeException();
        }
        List<ILootItem> iLootItems = commonDrops.computeIfAbsent(level, integer -> new ArrayList<>());
        iLootItems.add(lootItem);
        Map<String, Integer> weightMap = itemWeightMap.computeIfAbsent(lootItem, iLootItem -> new LinkedHashMap<>());
        weightMap.put(level, weight);
        save();
    }

    public List<ILootItem> inspect(int level) {
        return commonDrops.get(level);
    }

    public static ILootItem makeDrop(Player killer, IMob iMob) {
        World world = iMob.getEntity().getWorld();
        final Config config = InfPlugin.plugin.config();
        double overallShift = getShift(killer, config.lootOverallInc, config.lootOverallDec, config.lootOverallMax);
        double global = config.lootGlobal * (1+ (overallShift/100d));
        if (!RandomUtil.possibility(global / 100d)) {
            return null;
        }
        Map<ILootItem, Integer> loots = iMob.getLoots();
        if (loots.isEmpty()) {
            return null;
        }
        double dynamicShift = getShift(killer, config.lootDynamicInc, config.lootDynamicDec, config.lootDynamicMax);
        Map<ILootItem, Integer> balanced = balance(loots, overallShift, dynamicShift);
        return RandomUtil.weightedRandomPick(balanced);
    }

    public static ILootItem makeSpecialDrop(Player killer, IMob iMob) {
        Config config = InfPlugin.plugin.config();
        double overallShift = getShift(killer, config.lootOverallInc, config.lootOverallDec, config.lootOverallMax);
        double specialChance = iMob.getSpecialChance() * (1+ (overallShift/100d));
        if (!RandomUtil.possibility(specialChance / 100d)) {
            return null;
        }
        Map<ILootItem, Integer> specialLoots = iMob.getSpecialLoots();
        double dynamicShift = getShift(killer, config.lootDynamicInc, config.lootDynamicDec, config.lootDynamicMax);
        Map<ILootItem, Integer> balanced = balance(specialLoots, 0, dynamicShift);
        return RandomUtil.weightedRandomPick(balanced);
    }

    private static Map<ILootItem, Integer> balance(Map<ILootItem, Integer> loots, double overallShift, double dynamicShift) {
        Map<ILootItem, Integer> result = new LinkedHashMap<>(loots.size());
        if (loots.size() <= 0) {
            return result;
        }
        loots.forEach((item, value) -> {
            double weight = value;
            if (item.isDynamic()) {
                double temp = weight;
                temp *= ((dynamicShift / 100d) + 1);
                weight = (int) Math.ceil(temp);
            }
            result.put(item, (int) weight);
        });
        return result;
    }

    private static List<ICorrector> incs;
    private static List<ICorrector> decs;

    private static double getShift(Player killer, List<String> lootOverallInc, List<String> lootOverallDec, double lootOverallMax) {
        if (killer == null)return 0;
        try {
            if (incs == null || decs == null) {
                incs = CorrectionParser.parseStrs(lootOverallInc);
                decs = CorrectionParser.parseStrs(lootOverallDec);
            }
            AtomicDouble weightShift = new AtomicDouble(0);
            if (!incs.isEmpty()) {
                incs.forEach(iCorrection -> {
                    double correction = iCorrection.getCorrection(killer, killer.getInventory().getItemInMainHand());
                    if (correction > 0) {
                        weightShift.getAndAdd(correction);
                    }
                });
            }
            if (!decs.isEmpty()) {
                decs.forEach(iCorrection -> {
                    double correction = iCorrection.getCorrection(killer, killer.getInventory().getItemInMainHand());
                    if (correction > 0) {
                        weightShift.getAndAdd(-correction);
                    }
                });
            }
            return Math.max(weightShift.get(), lootOverallMax);
        } catch (Exception ex) {
            throw new IllegalConfigException();
        }
    }


    public static int getWeightForLevel(ILootItem lootItem, String level) {
        Map<ILootItem, Map<String, Integer>> itemWeightMap = instance.itemWeightMap;
        if (itemWeightMap.containsKey(lootItem)) {
            Map<String, Integer> levelWeightMap = itemWeightMap.get(lootItem);
            return levelWeightMap.getOrDefault(level, 0);
        } else {
            return 0;
        }
    }

    public void load() {
        lootConfig.load();
        incs = null;
        decs = null;
    }

    public void save() {
        lootConfig.save();
    }

    public static void serializeDrops(Map<String, ILootItem> lootItemMap, Map<String, LootConfig.LootWeight> lootMap) {
        if (instance == null) return;
        if (!instance.lootItemMap.isEmpty()) {
            lootItemMap.clear();
            lootItemMap.putAll(instance.lootItemMap);
        }
        if (!instance.commonDrops.isEmpty()) {
            lootMap.clear();
            instance.commonDrops.forEach((level, lootItems) -> {
                String levelStr = "level-" + level;
//                Map<String, Integer> levelLootMap = new LinkedHashMap<>(lootItems.size());
//                lootItems.forEach(iLootItem -> {
//                    levelLootMap.put(iLootItem.getName(), getWeightForLevel(iLootItem, level));
//                });
                LootConfig.LootWeight lootWeight = new LootConfig.LootWeight();
                lootItems.forEach(lootItem -> {
                    lootWeight.weightMap.put(lootItem.getName(), getWeightForLevel(lootItem, level));
                    lootMap.put(levelStr, lootWeight);
                });
            });
        }
    }

    public static void loadFromLootMap(Map<String, ILootItem> lootItemMap, Map<String, LootConfig.LootWeight> levels) {
        instance.lootItemMap = new LinkedHashMap<>(lootItemMap);
        if (!levels.isEmpty()) {
            levels.forEach((levelStr, lootWeight) -> {
                if (!levelStr.startsWith("level-")) {
                    return;
                }
                String[] split = levelStr.split("-");
                if (split.length != 2) {
                    instance.plugin.getLogger().log(Level.WARNING, "wrong loot.map.level \"" + levelStr + "\", skipping");
                    return;
                }
                try {
                    String level = split[1];
                    List<ILootItem> items = new ArrayList<>(lootWeight.weightMap.size());
                    lootWeight.weightMap.forEach((s, weight) -> {
                        ILootItem iLootItem = instance.lootItemMap.get(s);
                        if (iLootItem == null) {
                            instance.plugin.getLogger().log(Level.WARNING, "no item \"" + s + "\" found, skipping");
                            return;
                        }
                        Map<String, Integer> levelWeight = instance.itemWeightMap.computeIfAbsent(iLootItem, iLootItem1 -> new LinkedHashMap<>());
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

    public List<ILootItem> getLoots(String id) {
        return commonDrops.computeIfAbsent(id, integer -> new ArrayList<>());
    }

    public Collection<String> getLootNames() {
        return lootItemMap.keySet();
    }
}
