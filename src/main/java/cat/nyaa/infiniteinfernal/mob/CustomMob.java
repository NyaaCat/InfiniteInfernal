package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.abilitiy.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.controler.Aggro;
import cat.nyaa.infiniteinfernal.controler.InfAggroControler;
import cat.nyaa.infiniteinfernal.event.InfernalSpawnEvent;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CustomMob implements IMob {
    public static final NamespacedKey CUSTOM_MOB_BOSSBAR = new NamespacedKey(InfPlugin.plugin, "bossbar");
    private final MobConfig config;
    private List<ILootItem> commonLoots;
    private List<ILootItem> specialLoots;
    private List<IAbilitySet> abilities;
    private Map<LivingEntity, Aggro> nonPlayerTargets = new LinkedHashMap<>();
    private int level;
    private double specialChance;
    private boolean autoSpawn;
    private boolean dropVanilla;
    private EntityType entityType;
    private LivingEntity entity;
    private KeyedBossBar bossBar;
    private String name;
    private String taggedName;

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
                        Bukkit.getLogger().log(Level.WARNING, I18n.format("error.custom_mob.no_loot", s));
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
                    this.abilities.add();
                }catch (IllegalArgumentException e){
                    Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.abilities.bad_config", s));
                }
            });
        }
        //level
        this.level = level;
        this.autoSpawn = config.spawn.autoSpawn;
        this.dropVanilla = config.loot.vanilla;
        this.entityType = config.type;
        this.name = config.name;
        this.specialChance = config.loot.special.chance;
        this.taggedName = Utils.getTaggedName(pluginConfig.nameTag, name, level);
    }

    @Override
    public Map<ILootItem, Integer> getLoots() {
        Map<ILootItem, Integer> result = new LinkedHashMap<>(commonLoots.size());
        commonLoots.forEach(iLootItem -> result.put(iLootItem, iLootItem.getWeight(this.level)));
        return result;
    }

    @Override
    public Map<ILootItem, Integer> getSpecialLoots() {
        Map<ILootItem, Integer> result = new LinkedHashMap<>(commonLoots.size());
        specialLoots.forEach(iLootItem -> result.put(iLootItem, iLootItem.getWeight(this.level)));
        return result;
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
        return entityType;
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
    public double getMaxHealth() {
        return InfPlugin.plugin.config().levelConfigs.get(level).attr.health;
    }

    @Override
    public double getSpecialChance() {
        return specialChance;
    }

    @Override
    public void makeInfernal(LivingEntity entity) {
        this.entity = entity;
        if (entity.isDead()){
            MobManager.instance().removeMob(this);
            return;
        }
        entity.setCustomName(getTaggedName());
        entity.setCustomNameVisible(true);
        AttributeInstance damageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        Objects.requireNonNull(damageAttr);
        Objects.requireNonNull(maxHealthAttr);
        damageAttr.setBaseValue(getDamage());
        maxHealthAttr.setBaseValue(getMaxHealth());
        createBossbar(entity);
        InfernalSpawnEvent event = new InfernalSpawnEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            MobManager.instance().removeMob(this);
            entity.remove();
        }
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
        return dropVanilla;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTaggedName() {
        return taggedName;
    }

    @Override
    public void showParticleEffect() {
        LivingEntity entity = getEntity();
        World world = entity.getWorld();
        Location location = entity.getLocation();
        world.spawnParticle(Particle.LAVA, location, 10, 1);
        if (!(entity instanceof Player)){
            nonPlayerTargets.put(entity, new Aggro(entity));
        }
    }

    @Override
    public void autoRetarget() {
        new BukkitRunnable(){
            @Override
            public void run() {
                LivingEntity aggroTarget = new InfAggroControler().findAggroTarget(CustomMob.this);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        retarget(aggroTarget);
                    }
                }.runTask(InfPlugin.plugin);
            }
        }.runTaskAsynchronously(InfPlugin.plugin);
    }

    @Override
    public void retarget(LivingEntity entity) {
        LivingEntity mobEntity = getEntity();
        if (mobEntity instanceof Mob) {
            LivingEntity target = ((Mob) mobEntity).getTarget();
            if (target == null){
                if (entity==null)return;
                ((Mob) mobEntity).setTarget(entity);
                return;
            }
            if (target.equals(entity)){
                return;
            }
            ((Mob) mobEntity).setTarget(entity);
        }
    }

    @Override
    public LivingEntity getTarget() {
        return entity instanceof Mob ? ((Mob) entity).getTarget() : null;
    }

    @Override
    public List<LivingEntity> getNonPlayerTargets() {
        return null;
    }

    @Override
    public boolean isTarget(LivingEntity target) {
        LivingEntity mobTarget = getTarget();
        if (mobTarget == null)return false;
        return target.equals(mobTarget);
    }

    @Override
    public MobConfig getConfig() {
        return this.config;
    }

    @Override
    public void onDeath() {
        MobManager.instance().removeMob(this);
    }
}
