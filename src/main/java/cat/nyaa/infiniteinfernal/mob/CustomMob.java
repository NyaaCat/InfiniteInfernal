package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;
import cat.nyaa.infiniteinfernal.configs.LevelConfig;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CustomMob implements IMob {
    public static final NamespacedKey CUSTOM_MOB_BOSSBAR = new NamespacedKey(InfPlugin.plugin, "bossbar");
    private final MobConfig config;
    private List<ILootItem> commonLoots;
    private List<ILootItem> specialLoots;
    private List<IAbility> abilities;
    private int level;
    private boolean autoSpawn;
    private LivingEntity entity;
    private KeyedBossBar bossBar;

    public CustomMob(MobConfig config, int level){
        this.config = config;
        generateFromConfig(config, level);
    }

    private void generateFromConfig(MobConfig config, int level) {
        Config pluginConfig = InfPlugin.plugin.config();
        //common loots
        commonLoots = new ArrayList<>();
        if (config.loot.imLoot){
            commonLoots = LootManager.instance().getLevelDrops(level);
        }
        //special loots
        specialLoots = config.loot.special.list.stream()
                .map(s -> {
                    ILootItem loot = LootManager.instance().getLoot(s);
                    if (loot != null){
                        return loot;
                    }else {
                        //todo: log error
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //abilities
        abilities = new ArrayList<>();
        if (!config.abilities.isEmpty()) {
            config.abilities.forEach(s -> {
                try {
                    AbilitySetConfig abilitySetConfig = pluginConfig.abilityConfigs.parseId(s);
                    Map<String, IAbility> abilities = abilitySetConfig.abilities;
                    this.abilities.addAll(abilities.values());
                }catch (IllegalArgumentException e){
                    //todo: log
                }
            });
        }
        //level
        this.level = level;
        this.autoSpawn = config.spawn.autoSpawn;
    }

    @Override
    public List<ILootItem> getLoots() {
        return commonLoots;
    }

    @Override
    public List<ILootItem> getSpecialLoots() {
        return specialLoots;
    }

    @Override
    public List<IAbility> getAbilities() {
        return abilities;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public EntityType getEntityType() {
        return null;
    }

    @Override
    public KeyedBossBar getBossBar() {
        return bossBar;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public double getDamage() {
        return InfPlugin.plugin.config().levelConfigs.get(level).attr.damage;
    }

    @Override
    public void makeInfernal(LivingEntity entity) {
        this.entity = entity;
        createBossbar(entity);
    }

    private void createBossbar(LivingEntity entity) {
        String customName = entity.getCustomName();
        if (customName == null || customName.equals("")){
            customName = entity.getName();
        }
        bossBar = Bukkit.createBossBar(CUSTOM_MOB_BOSSBAR, customName, BarColor.BLUE, BarStyle.SEGMENTED_10);
    }

    @Override
    public boolean isAutoSpawn() {
        return autoSpawn;
    }

    @Override
    public boolean dropVanilla() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getTaggedName() {
        return null;
    }
}
