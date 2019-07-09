package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.IllegalConfigException;
import cat.nyaa.infiniteinfernal.configs.WorldConfig.LootingConfig;
import cat.nyaa.infiniteinfernal.configs.WorldConfig.LootingConfig.LootingModifiers;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    public void addLoot(String name, boolean dynamic, ItemStack itemStack) {
        ILootItem lootItem;
        if (itemStack.getType().equals(Material.ENCHANTED_BOOK)) {
            lootItem = new EnchantmentBookLootItem(plugin, name, itemStack);
        } else {
            lootItem = new CommonLootItem(plugin, name, itemStack);
        }
        lootItem.setDynamic(dynamic);
        lootItemMap.put(name, lootItem);
    }

    public ILootItem getLoot(String name) {
        return lootItemMap.get(name);
    }

    public void setDrop(String item, int level, int weight) {
        ILootItem lootItem = lootItemMap.get(item);
        if (lootItem == null) {
            throw new RuntimeException();
        }
        List<ILootItem> iLootItems = commonDrops.computeIfAbsent(level, integer -> new ArrayList<>());
        iLootItems.add(lootItem);
        Map<Integer, Integer> weightMap = itemWeightMap.computeIfAbsent(lootItem, iLootItem -> new LinkedHashMap<>());
        weightMap.put(level, weight);
    }

    public List<ILootItem> inspect(int level) {
        return commonDrops.get(level);
    }

    public static ILootItem makeDrop(Player killer, IMob iMob) {
        World world = killer.getWorld();
        LootingConfig lootCfg = InfPlugin.plugin.config().worlds.get(world.getName()).looting;
        int overallShift = getShift(killer, lootCfg.overall);
        double global = lootCfg.global + overallShift;
        if (!Utils.possibility(global / 100d)) {
            return null;
        }
        Map<ILootItem, Integer> loots = iMob.getLoots();
        if (loots.isEmpty()) {
            return null;
        }
        int dynamicShift = getShift(killer, lootCfg.dynamic);
        Map<ILootItem, Integer> balanced = balance(loots, overallShift, dynamicShift);
        return Utils.weightedRandomPick(balanced);
    }

    public static ILootItem makeSpecialDrop(Player killer, IMob iMob) {
        double specialChance = iMob.getSpecialChance();
        if (!Utils.possibility(specialChance)) {
            return null;
        }
        World world = killer.getWorld();
        LootingConfig lootCfg = InfPlugin.plugin.config().worlds.get(world.getName()).looting;
        Map<ILootItem, Integer> specialLoots = iMob.getSpecialLoots();
        int dynamicShift = getShift(killer, lootCfg.dynamic);
        Map<ILootItem, Integer> balanced = balance(specialLoots, 0, dynamicShift);
        return Utils.weightedRandomPick(balanced);
    }

    private static Map<ILootItem, Integer> balance(Map<ILootItem, Integer> loots, int overallShift, int dynamicShift) {
        Map<ILootItem, Integer> result = new LinkedHashMap<>(loots.size());
        if (loots.size() <= 0) {
            return result;
        }
        loots.forEach((item, value) -> {
            int weight = value + overallShift;
            if (item.isDynamic()) {
                double temp = weight;
                temp *= ((dynamicShift / 100d) + 1);
                weight = (int) Math.ceil(temp);
            }
            result.put(item, weight);
        });
        return result;
    }

    private static int getShift(Player killer, LootingModifiers overall) {
        List<String> incEffectStrs = overall.inc.effect;
        List<String> incEnchantStrs = overall.inc.enchant;
        List<String> decEffectStrs = overall.dec.effect;
        List<String> decEnchantStrs = overall.dec.enchant;
        try {
            Map<PotionEffectType, Integer> incEffects = parseEffect(incEffectStrs);
            Map<PotionEffectType, Integer> decEffects = parseEffect(decEffectStrs);
            Map<Enchantment, Integer> incEnchants = parseEnchant(incEnchantStrs);
            Map<Enchantment, Integer> decEnchants = parseEnchant(decEnchantStrs);

            Collection<PotionEffect> activePotionEffects = killer.getActivePotionEffects();
            Map<Enchantment, Integer> enchantments = killer.getInventory().getItemInMainHand().getEnchantments();
            AtomicInteger weightShift = new AtomicInteger();
            activePotionEffects.stream().parallel()
                    .forEach(potionEffect -> {
                        PotionEffectType type = potionEffect.getType();
                        if (incEffects.containsKey(type)) {
                            weightShift.addAndGet(incEffects.get(type) * potionEffect.getAmplifier());
                        }
                        if (decEffects.containsKey(type)) {
                            weightShift.addAndGet(-(decEffects.get(type) * potionEffect.getAmplifier()));
                        }
                    });
            enchantments.forEach((enchantment, level) -> {
                if (incEnchants.containsKey(enchantment)) {
                    weightShift.addAndGet(incEnchants.get(enchantment) * level);
                }
                if (decEnchants.containsKey(enchantment)) {
                    weightShift.addAndGet(-(decEnchants.get(enchantment) * level));
                }
            });
            return weightShift.get();
        } catch (Exception ex) {
            throw new IllegalConfigException();
        }
    }

    private static Map<PotionEffectType, Integer> parseEffect(List<String> effect) {
        Map<PotionEffectType, Integer> effects = new LinkedHashMap<>(effect.size());
        if (!effect.isEmpty()) {
            effect.forEach(s -> {
                String[] split = s.split(":");
                PotionEffectType effectType = PotionEffectType.getByName(split[0]);
                effects.put(effectType, Integer.valueOf(split[1]));
            });
        }
        return effects;
    }

    private static Map<Enchantment, Integer> parseEnchant(List<String> enchant) {
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>(enchant.size());
        if (!enchant.isEmpty()) {
            enchant.forEach(s -> {
                String[] split = s.split(":");
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(split[0]));
                enchants.put(enchantment, Integer.valueOf(split[1]));
            });
        }
        return enchants;
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
        if (!instance.lootItemMap.isEmpty()) {
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
