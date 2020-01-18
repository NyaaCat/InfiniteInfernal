package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.AbilityDummy;
import cat.nyaa.infiniteinfernal.ability.IAbility;
import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.utils.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static cat.nyaa.infiniteinfernal.ability.AbilityCollection.ACTIVE_CLASSES;
import static cat.nyaa.infiniteinfernal.ability.AbilityCollection.PASSIVE_CLASSES;

public class Config extends PluginConfigure {
    InfPlugin plugin;

    Config(InfPlugin plugin) {
        this.plugin = plugin;
        abilityConfigs = new NamedDirConfigs<>(new File(plugin.getDataFolder(), "abilities"), AbilitySetConfig.class);
        levelConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "levels"), LevelConfig.class);
        mobConfigs = new NamedDirConfigs<>(new File(plugin.getDataFolder(), "mobs"), MobConfig.class);
        regionConfigs = new NamedDirConfigs<>(new File(plugin.getDataFolder(), "regions"), RegionConfig.class);
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Serializable
    public String language = "en_US";

    @Serializable
    public String nameTag = "[INFERNAL] {level.prefix} {mob.type} Level {level.level}";

    @Serializable
    public BossbarConfig bossbar = new BossbarConfig();

    @Serializable
    public List<String> tags = new ArrayList<>();

    @Serializable
    public int groupShareRange = 50;

    @Serializable
    public Map<String, WorldConfig> worlds = new LinkedHashMap<>();

    @Serializable(name = "GetDropMessageFeedback")
    public boolean isGetDropMessageEnabled = false;

    @Serializable
    public Map<String, String> addEffects = new LinkedHashMap<>();

    //<STANDALONE CONFIGS>
    public NamedDirConfigs<AbilitySetConfig> abilityConfigs;
    public DirConfigs<LevelConfig> levelConfigs;
    public NamedDirConfigs<MobConfig> mobConfigs;
    public NamedDirConfigs<RegionConfig> regionConfigs;

    private void saveStandaloneConfigs() {
        abilityConfigs.saveToDir();
        levelConfigs.saveToDir();
        mobConfigs.saveToDir();
        regionConfigs.saveToDir();
    }

    private void loadStandaloneConfigs() {
        abilityConfigs.loadFromDir();
        levelConfigs.loadFromDir();
        mobConfigs.loadFromDir();
        regionConfigs.loadFromDir();
    }
    //<STANDALONE CONFIGS/>

    @Override
    public void load() {
        getPlugin().saveDefaultConfig();
        getPlugin().reloadConfig();
        deserialize(getPlugin().getConfig());
        abilityConfigs.clear();
        levelConfigs.clear();
        mobConfigs.clear();
        regionConfigs.clear();
        addEffectInstance = null;

        if (worlds.size() == 0) {
            Bukkit.getLogger().log(Level.INFO, "first time using Infinite Infernal, initializing...");
            initConfigs();
        }
        generateConfigForWorlds();
        this.loadStandaloneConfigs();
        save();
    }

    private void generateConfigForWorlds() {
        List<World> worlds = Bukkit.getWorlds();
        if (!worlds.isEmpty()) {
            String inc = "attribute:GENERIC_LUCK:10";
            String dec = "effect:UNLUCK:5";
            worlds.forEach(world -> {
                if (this.worlds.get(world.getName()) == null) {
                    WorldConfig value = new WorldConfig(InfPlugin.plugin);
                    value.looting.dynamic.dec.add(dec);
                    value.looting.dynamic.inc.add(inc);
                    value.looting.overall.dec.add(dec);
                    value.looting.overall.inc.add(inc);
                    this.worlds.put(world.getName(), value);
                }
            });
        } else {
            Bukkit.getLogger().log(Level.SEVERE, "No world detected!");
        }
    }

    private void initConfigs() {
        tags.add("[infernal] ");
        for (int i = 0; i < 12; i++) {
            LevelConfig config = new LevelConfig(i);
            levelConfigs.add(config);
            config.level = i;
            config.spawnConfig.weight = i;
            config.spawnConfig.from = 200 * i;
            config.spawnConfig.to = 200 * (i + 1);
            config.attr.aggro = 10 + 0.5 * i;
            config.attr.damage = 20 * i;
            config.attr.damageResist = 0;
            config.attr.exp = 10 * i;
            config.attr.health = 20 * i;
            config.prefix = "level " + i;
        }
        addEffects.put("target_lost", "effect:BLINDNESS:10");
        addEffects.put("disorder", "effect:CONFUSION:10");
        addEffects.put("dementia", "effect:SLOW_DIGGING:10");
        AbilitySetConfig actives = new AbilitySetConfig("a");
        AbilitySetConfig passives = new AbilitySetConfig("b");
        AbilitySetConfig dummies = new AbilitySetConfig("c");
        addAbilities(actives, ACTIVE_CLASSES);
        addAbilities(passives, PASSIVE_CLASSES);
        dummies.abilities.put("dummy", new AbilityDummy());
        abilityConfigs.add(actives);
        abilityConfigs.add(passives);
        abilityConfigs.add(dummies);
        ItemStack sampleItem = new ItemStack(Material.ACACIA_BUTTON);
        ItemMeta itemMeta = sampleItem.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName("inf-sample");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("inf-sample");
            itemMeta.setLore(lore);
            itemMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            itemMeta.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier("luck-1", 1, AttributeModifier.Operation.ADD_NUMBER));
            sampleItem.setItemMeta(itemMeta);
        }
        ItemStack extraSampleItem = new ItemStack(Material.ACACIA_BUTTON);
        ItemMeta itemMeta1 = sampleItem.getItemMeta();
        if (itemMeta1 != null) {
            itemMeta1.setDisplayName("inf-extra-sample");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("inf-extra-sample");
            itemMeta1.setLore(lore);
            itemMeta1.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            itemMeta1.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier("luck-1", 1, AttributeModifier.Operation.ADD_NUMBER));
            extraSampleItem.setItemMeta(itemMeta1);
        }
        ItemStack sampleItem5 = new ItemStack(Material.ACACIA_BUTTON);
        ItemMeta itemMeta2 = sampleItem.getItemMeta();
        if (itemMeta2 != null) {
            itemMeta2.setDisplayName("inf-sample-5");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("inf-sample-5");
            itemMeta2.setLore(lore);
            itemMeta2.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            itemMeta2.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier("luck-1", 1, AttributeModifier.Operation.ADD_NUMBER));
            sampleItem5.setItemMeta(itemMeta2);
        }
        ItemStack sampleItem10 = new ItemStack(Material.ACACIA_BUTTON);
        ItemMeta itemMeta3 = sampleItem.getItemMeta();
        if (itemMeta3 != null) {
            itemMeta3.setDisplayName("inf-sample-10");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("inf-sample-10");
            itemMeta3.setLore(lore);
            itemMeta3.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            itemMeta3.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier("luck-1", 1, AttributeModifier.Operation.ADD_NUMBER));
            sampleItem10.setItemMeta(itemMeta3);
        }
        ItemStack sampleItem20 = new ItemStack(Material.ACACIA_BUTTON);
        ItemMeta itemMeta4 = sampleItem.getItemMeta();
        if (itemMeta4 != null) {
            itemMeta4.setDisplayName("inf-sample-20");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("inf-sample-20");
            itemMeta4.setLore(lore);
            itemMeta4.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            itemMeta4.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier("luck-1", 1, AttributeModifier.Operation.ADD_NUMBER));
            sampleItem20.setItemMeta(itemMeta4);
        }
        LootManager.instance().addLoot("inf-sample-10", true, sampleItem10);
        LootManager.instance().addLoot("inf-sample-20", true, sampleItem20);
        LootManager.instance().addLoot("inf-sample-5", true, sampleItem5);
        LootManager.instance().setDrop("inf-sample-10", 1, 10);
        LootManager.instance().setDrop("inf-sample-20", 1, 20);
        LootManager.instance().setDrop("inf-sample-5", 1, 5);
        LootManager.instance().setDrop("inf-sample-10", 2, 10);
        LootManager.instance().setDrop("inf-sample-20", 2, 20);
        LootManager.instance().setDrop("inf-sample-5", 2, 5);
        LootManager.instance().setDrop("inf-sample-10", 3, 10);
        LootManager.instance().setDrop("inf-sample-20", 3, 20);
        LootManager.instance().setDrop("inf-sample-5", 3, 5);
        LootManager.instance().addLoot("inf-extra-sample", true, extraSampleItem);
        MobConfig mobConfig = new MobConfig("sample");
        mobConfig.type = EntityType.ZOMBIE;
        mobConfig.abilities.add(actives.getPrefix() + "-" + actives.getName());
        mobConfig.abilities.add(passives.getPrefix() + "-" + passives.getName());
        mobConfig.abilities.add("set-2");
        mobConfig.name = "Zombie-King";
        for (Biome value : Biome.values()) {
            mobConfig.spawn.biomes.add(value.name());
        }
        mobConfig.spawn.worlds.addAll(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()));
        mobConfig.spawn.levels.add("1-3");
        mobConfig.spawn.levels.add("5");
        mobConfig.loot.vanilla = false;
        mobConfig.loot.special.list.add("inf-extra-sample:10");
        mobConfigs.add(mobConfig);
        Bukkit.getWorlds().stream().forEach(world -> {
            RegionConfig config = new RegionConfig("sample-region", new RegionConfig.Region(new Location(world, 0, 0, 0), new Location(world, 100, 100, 100)));
            config.mobs.add("mob-0:10");
            regionConfigs.add(config);
        });
        save();
    }

    private void addAbilities(AbilitySetConfig actives, Class<? extends IAbility>[] activeClasses) {
        if (activeClasses.length > 0) {
            for (Class<? extends IAbility> abilityClass : activeClasses) {
                try {
                    Constructor<? extends IAbility> constructor = abilityClass.getConstructor();
                    IAbility o = constructor.newInstance();
                    actives.abilities.put(o.getName(), o);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void save() {
        super.save();
        this.saveStandaloneConfigs();
    }

    public List<RegionConfig> getRegionsForLocation(Location location) {
        return regionConfigs.values().stream()
                .filter(regionConfig1 -> regionConfig1.region != null && regionConfig1.region.contains(location))
                .collect(Collectors.toList());
    }

    private Map<String, ICorrector> addEffectInstance = null;

    public Map<String, ICorrector> getAddEffects() {
        if (addEffectInstance == null) {
            initAddEffectInstance();
        }
        return addEffectInstance;
    }

    public ICorrector getAddEffect(String name) {
        if (addEffectInstance == null) {
            initAddEffectInstance();
        }
        return addEffectInstance.get(name);
    }

    private void initAddEffectInstance() {
        addEffectInstance = new LinkedHashMap<>();
        addEffects.forEach(((s, s2) -> {
            ICorrector iCorrector = CorrectionParser.parseStr(s);
            if (iCorrector != null) {
                addEffectInstance.put(s, iCorrector);
            }
        }));
    }
}
