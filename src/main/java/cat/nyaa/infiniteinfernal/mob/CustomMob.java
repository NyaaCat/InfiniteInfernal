package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilitySet;
import cat.nyaa.infiniteinfernal.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.configs.AbilitySetConfig;
import cat.nyaa.infiniteinfernal.configs.LevelConfig;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.configs.WorldConfig;
import cat.nyaa.infiniteinfernal.controler.Aggro;
import cat.nyaa.infiniteinfernal.controler.InfAggroController;
import cat.nyaa.infiniteinfernal.event.InfernalSpawnEvent;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.utils.NmsUtils;
import com.udojava.evalex.Expression;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;

public class CustomMob implements IMob {
    public static final NamespacedKey CUSTOM_MOB_BOSSBAR = new NamespacedKey(InfPlugin.plugin, "bossbar");
    private final MobConfig config;
    private List<ILootItem> commonLoots;
    private Map<ILootItem, Integer> specialLoots;
    private List<IAbilitySet> abilities;
    private Map<LivingEntity, Aggro> nonPlayerTargets = new LinkedHashMap<>();
    private int level;
    private double specialChance;
    private boolean autoSpawn;
    private boolean dropVanilla;
    private EntityType entityType;
    private LivingEntity entity;
    private KeyedBossBar bossBar;
    private LivingEntity currentTarget = null;
    private String name;
    private String taggedName;
    private EntityDamageEvent LastDamageCause = null;
    private double health;
    private boolean enableDynamicHealth;
    private String dynamicHealthExpression = "";
    private double followDistance = 48;

    public CustomMob(MobConfig config, int level) {
        this.config = config;
        generateFromConfig(config, level);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomMob customMob = (CustomMob) o;
        return Objects.equals(entity, customMob.entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity);
    }

    private void generateFromConfig(MobConfig config, int level) {
        Config pluginConfig = InfPlugin.plugin.config();
        //common loots
        commonLoots = new ArrayList<>();
        if (config.loot.imLoot) {
            commonLoots = LootManager.instance().getLevelDrops(level);
        }
        //special loots
        specialLoots = new LinkedHashMap<>();
        config.loot.special.list.stream()
                .forEach(s -> {
                    String[] split = s.split(":", 2);
                    String loot = split[0];
                    int weight = Integer.parseInt(split[1]);
                    ILootItem iLoot = LootManager.instance().getLoot(loot);
                    if (iLoot != null) {
                        specialLoots.put(iLoot, weight);
                    } else {
                        Bukkit.getLogger().log(Level.WARNING, I18n.format("error.custom_mob.no_loot", s));
                    }
                });
        //abilities
        abilities = new ArrayList<>();
        if (!config.abilities.isEmpty()) {
            config.abilities.forEach(s -> {
                try {
                    AbilitySetConfig abilitySetConfig = pluginConfig.abilityConfigs.get(s);
                    if (abilitySetConfig == null) {
                        Bukkit.getLogger().log(Level.WARNING, "no ability config for " + s);
                        return;
                    }
                    this.abilities.add(new AbilitySet(abilitySetConfig));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().log(Level.WARNING, I18n.format("error.abilities.bad_config", s));
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
        this.taggedName = Utils.getTaggedName(pluginConfig.nameTag, entityType, name, level);
        double healthOverride = config.healthOverride;
        if (healthOverride > 0){
            health = healthOverride;
        }else {
            health = InfPlugin.plugin.config().levelConfigs.get(level).attr.health;
        }
        enableDynamicHealth = config.enableDynamicHealth;
        dynamicHealthExpression = config.dynamicHealthExpression;
    }

    @Override
    public Map<ILootItem, Integer> getLoots() {
        Map<ILootItem, Integer> result = new LinkedHashMap<>(commonLoots.size());
        commonLoots.forEach(iLootItem -> result.put(iLootItem, iLootItem.getWeight(this.level)));
        return result;
    }

    @Override
    public Map<ILootItem, Integer> getSpecialLoots() {
        return new LinkedHashMap<>(specialLoots);
    }

    @Override
    public List<IAbilitySet> getAbilities() {
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
        return health;
    }

    @Override
    public double getSpecialChance() {
        return specialChance;
    }

    @Override
    public void makeInfernal(LivingEntity entity) {
        this.entity = entity;
        if (entity.isDead()) {
            MobManager.instance().removeMob(this, false);
            return;
        }
        entity.setCustomName(getTaggedName());
        entity.setCustomNameVisible(true);
        AttributeInstance damageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance followRangeAttr = entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(getDamage());
        } else {
        }
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(getMaxHealth());
        } else {
        }
        if (followRangeAttr != null) {
            World entityWorld = entity.getWorld();
            WorldConfig worldConfig = InfPlugin.plugin.config().worlds.get(entityWorld.getName());
            LevelConfig levelConfig = InfPlugin.plugin.config().levelConfigs.get(getLevel());
            double aggro = levelConfig.attr.aggro;
            if (entityType.equals(EntityType.GUARDIAN) || entityType.equals(EntityType.ELDER_GUARDIAN)) {
                followDistance = aggro * 0.60;
            }else {
                followDistance = worldConfig.aggro.range.max;
            }
            followRangeAttr.setBaseValue(followDistance);
        } else {
        }
        entity.setHealth(getMaxHealth());
        createBossbar(entity);
        InfernalSpawnEvent event = new InfernalSpawnEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            MobManager.instance().removeMob(this, false);
            entity.remove();
            return;
        }
        if (config.nbtTags != null && !config.nbtTags.equals("")) {
            NmsUtils.setEntityTag(entity, config.nbtTags);
        }
    }

    private void createBossbar(LivingEntity entity) {
        String customName = getTaggedName();
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
    public boolean isDynamicHealth() {
        return enableDynamicHealth;
    }

    @Override
    public String getName() {
        return config.name;
    }

    @Override
    public String getTaggedName() {
        return ChatColor.translateAlternateColorCodes('&', taggedName);
    }

    @Override
    public void showParticleEffect() {
        LivingEntity entity = getEntity();
        World world = entity.getWorld();
        Location location = entity.getLocation();
        world.spawnParticle(Particle.LAVA, location, 10, 0, 0, 0, 1, null, true);
    }

    @Override
    public void autoRetarget() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity aggroTarget = new InfAggroController().findAggroTarget(CustomMob.this);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        retarget(aggroTarget);
                    }
                }.runTask(InfPlugin.plugin);
            }
        }.runTask(InfPlugin.plugin);
    }

    @Override
    public void retarget(LivingEntity entity) {
        LivingEntity mobEntity = getEntity();
        if (mobEntity instanceof Mob) {
            LivingEntity target = ((Mob) mobEntity).getTarget();
            if (target == null) {
                if (entity == null) return;
                ((Mob) mobEntity).setTarget(entity);
                this.currentTarget = entity;
                return;
            }
            if (target.equals(entity)) {
                return;
            }
            ((Mob) mobEntity).setTarget(entity);
            this.currentTarget = entity;
        }
    }

    @Override
    public void tweakHealth() {
        if (getEntity() == null || dynamicHealthExpression.equals("")) return;
        Expression expression = createExpression();
        BigDecimal maxHealth = expression.eval();
        double originHealth = getEntity().getHealth();
        AttributeInstance maxHealthAttr = getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr == null) return;
        double originMax = maxHealthAttr.getValue();
        if (maxHealth.doubleValue() <= originMax)return;

        double originPercentile = originHealth / originMax;
        maxHealthAttr.setBaseValue(maxHealth.doubleValue());
        getEntity().setHealth(originPercentile * maxHealth.doubleValue());
    }

    private Expression createExpression(){
        MobManager instance = MobManager.instance();
        Expression expression = new Expression(dynamicHealthExpression);
        expression.and("health", new NamedLazyNumber("health", getEntity().getHealth()));
        expression.and("maxHealth", new NamedLazyNumber("maxHealth", health));
        expression.and("playerCount", new NamedLazyNumber("playerCount", instance.getPlayersNearMob(this).stream()
                .filter(player -> player.getLocation().distance(this.getEntity().getLocation()) <= this.followDistance)
                .count())
        );
        expression.and("aggroRange", new NamedLazyNumber("aggroRange", followDistance));
        return expression;
    }

    private boolean checkExpression(){
        try{
            MobManager instance = MobManager.instance();
            Expression expression = new Expression(dynamicHealthExpression);
            expression.and("health", new NamedLazyNumber("health", getEntity().getHealth()));
            expression.and("maxHealth", new NamedLazyNumber("maxHealth", health));
            expression.and("playerCount", new NamedLazyNumber("playerCount", instance.getPlayersNearMob(this).stream()
                    .filter(player -> player.getLocation().distance(this.getEntity().getLocation()) <= this.followDistance)
                    .count())
            );
            expression.and("aggroRange", new NamedLazyNumber("aggroRange", followDistance));
            BigDecimal eval = expression.eval();
            return Double.isFinite(eval.doubleValue());
        } catch (Exception e){
            return false;
        }
    }

    @Override
    public LivingEntity getTarget() {
//        return entity instanceof Mob ? ((Mob) entity).getTarget() : null;
        return currentTarget;
    }

    @Override
    public Map<LivingEntity, Aggro> getNonPlayerTargets() {
        return nonPlayerTargets;
    }

    @Override
    public boolean isTarget(LivingEntity target) {
        LivingEntity mobTarget = getTarget();
        if (mobTarget == null) return false;
        return target.equals(mobTarget);
    }

    @Override
    public MobConfig getConfig() {
        return this.config;
    }

    @Override
    public void onDeath() {
        MobManager.instance().removeMob(this, true);
    }

    @Override
    public int getExp() {
        int exp = config.loot.expOverride;
        if (exp == -1) {
            LevelConfig levelConfig = InfPlugin.plugin.config().levelConfigs.get(getLevel());
            if (levelConfig == null) {
                Bukkit.getLogger().log(Level.WARNING, "no level config for \"" + getLevel() + "\"");
                exp = 0;
            } else {
                exp = levelConfig.attr.exp;
            }
        }
        return exp;
    }

    @Override
    public EntityDamageEvent getLastDamageCause() {
        return LastDamageCause;
    }

    @Override
    public void setLastDamageCause(EntityDamageEvent event) {
        LastDamageCause = event;
    }

    @Override
    public void updateBossBar(KeyedBossBar bossBar, LivingEntity entity) {
        double health = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double progress = health / maxHealth;
        bossBar.setProgress(Math.min(Math.max(0, progress), Math.min(progress, 1)));
        if (progress < 0.33) {
            bossBar.setColor(BarColor.RED);
        } else if (progress < 0.66) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.BLUE);
        }
    }

    private class NamedLazyNumber implements Expression.LazyNumber{
        private final String name;
        private final double val;

        public NamedLazyNumber(String name, double val){
            this.name = name;
            this.val = val;
        }

        @Override
        public BigDecimal eval() {
            return new BigDecimal(val);
        }

        @Override
        public String getString() {
            return null;
        }
    }
}
