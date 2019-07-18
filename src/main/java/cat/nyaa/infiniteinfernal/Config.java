package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.ability.AbilityDummy;
import cat.nyaa.infiniteinfernal.ability.IAbility;
import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.nyaacore.configuration.PluginConfigure;
import cat.nyaa.nyaacore.utils.ClassPathUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
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

public class Config extends PluginConfigure {
    InfPlugin plugin;

    Config(InfPlugin plugin) {
        this.plugin = plugin;
        abilityConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "abilities"), AbilitySetConfig.class);
        levelConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "levels"), LevelConfig.class);
        mobConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "mobs"), MobConfig.class);
        regionConfigs = new DirConfigs<>(new File(plugin.getDataFolder(), "regions"), RegionConfig.class);
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Serializable
    public String language = "en_US";

    @Serializable
    public String nameTag = "[INFERNAL] {level.prefix} {mob.type} Level {Level.level}";

    @Serializable
    public BossbarConfig bossbar = new BossbarConfig();

    @Serializable
    public List<String> tags = new ArrayList<>();

    @Serializable
    public Map<String, WorldConfig> worlds = new LinkedHashMap<>();

    //<STANDALONE CONFIGS>
    public DirConfigs<AbilitySetConfig> abilityConfigs;
    public DirConfigs<LevelConfig> levelConfigs;
    public DirConfigs<MobConfig> mobConfigs;
    public DirConfigs<RegionConfig> regionConfigs;

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
        super.load();
        if (worlds.size() == 0) {
            Bukkit.getLogger().log(Level.INFO, "first time using Infinite Infernal, initializing...");
            initConfigs();
        }
        generateConfigForWorlds();
        this.loadStandaloneConfigs();
    }

    private void generateConfigForWorlds() {
        List<World> worlds = Bukkit.getWorlds();
        if (!worlds.isEmpty()) {
            String inc = "attribute:LUCK:10";
            String dec = "attribute:UNLUCK:5";
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
            save();
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
            config.prefix = "level "+i;
        }
        AbilitySetConfig actives = new AbilitySetConfig(0);
        AbilitySetConfig passives = new AbilitySetConfig(1);
        AbilitySetConfig dummies = new AbilitySetConfig(2);
        Class<? extends IAbility>[] activeClasses = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.ability.impl.active", IAbility.class);
        Class<? extends IAbility>[] passiveClasses = ClassPathUtils.scanSubclasses(InfPlugin.plugin, "cat.nyaa.infiniteinfernal.ability.impl.passive", IAbility.class);
        addAbilities(actives, activeClasses);
        addAbilities(passives, passiveClasses);
        dummies.abilities.put("dummy", new AbilityDummy());
        abilityConfigs.add(actives);
        abilityConfigs.add(passives);
        abilityConfigs.add(dummies);
        ItemStack sampleItem = new ItemStack(Material.ACACIA_BOAT);
        ItemMeta itemMeta = sampleItem.getItemMeta();
        if (itemMeta!=null){
            itemMeta.setDisplayName("inf-sample");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("inf-sample");
            itemMeta.setLore(lore);
            itemMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            itemMeta.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier("luck-1", 1, AttributeModifier.Operation.ADD_NUMBER));
            sampleItem.setItemMeta(itemMeta);
        }
        ItemStack extraSampleItem = new ItemStack(Material.ACACIA_BOAT);
        ItemMeta itemMeta1 = sampleItem.getItemMeta();
        if (itemMeta1!=null){
            itemMeta1.setDisplayName("inf-extra-sample");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("inf-extra-sample");
            itemMeta1.setLore(lore);
            itemMeta1.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            itemMeta1.addAttributeModifier(Attribute.GENERIC_LUCK, new AttributeModifier("luck-1", 1, AttributeModifier.Operation.ADD_NUMBER));
            extraSampleItem.setItemMeta(itemMeta1);
        }
        LootManager.instance().addLoot("inf-sample", true, sampleItem);
        LootManager.instance().setDrop("inf-sample", 1, 10);
        LootManager.instance().addLoot("inf-extra-sample", true, extraSampleItem);
        LootManager.instance().setDrop("inf-extra-sample", 1, 10);
        MobConfig mobConfig = new MobConfig(0);
        mobConfig.type = EntityType.ZOMBIE;
        mobConfig.abilities.add(actives.getPrefix()+"-"+actives.getId());
        mobConfig.abilities.add(actives.getPrefix()+"-"+actives.getId());
        mobConfig.abilities.add("set-3");
        mobConfig.name = "Zombie-King";
        mobConfig.spawn.biomes.add("PLAINS");
        mobConfig.spawn.worlds.addAll(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()));
        mobConfig.spawn.levels.add("1-3");
        mobConfig.spawn.levels.add("5");
        mobConfig.loot.special.list.add("inf-sample:10");
        mobConfig.loot.vanilla = false;
        mobConfig.loot.special.list.add("inf-extra-sample:10");
        mobConfigs.add(mobConfig);
        Bukkit.getWorlds().stream().forEach(world -> {
            regionConfigs.add(new RegionConfig(0, new RegionConfig.Region(new Location(world, 0,0,0), new Location(world, 100,100,100))));
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
                .filter(regionConfig1 -> regionConfig1.region!=null && regionConfig1.region.contains(location))
                .collect(Collectors.toList());
    }
}
