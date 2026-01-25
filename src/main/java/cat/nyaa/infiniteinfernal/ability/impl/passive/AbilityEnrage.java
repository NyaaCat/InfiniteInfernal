package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityEnrage - 狂暴化
 *
 * 生命值低于阈值时进入狂暴状态，获得大幅增益。
 */
public class AbilityEnrage extends AbilityPassive implements AbilityHurt {

    // 追踪已触发狂暴的怪物
    private static final Set<UUID> enragedMobs = ConcurrentHashMap.newKeySet();

    // === 配置参数 ===

    @Serializable
    public double healthThreshold = 0.3;  // 触发阈值 30% 血量

    // 狂暴效果配置
    @Serializable
    public String enrageEffect1 = "STRENGTH";
    @Serializable
    public int enrageEffect1Duration = -1;  // -1 表示永久
    @Serializable
    public int enrageEffect1Amplifier = 1;

    @Serializable
    public String enrageEffect2 = "SPEED";
    @Serializable
    public int enrageEffect2Duration = -1;
    @Serializable
    public int enrageEffect2Amplifier = 1;

    @Serializable
    public String enrageEffect3 = "RESISTANCE";
    @Serializable
    public int enrageEffect3Duration = -1;
    @Serializable
    public int enrageEffect3Amplifier = 0;

    @Serializable
    public boolean triggerOnce = true;  // 只触发一次

    @Serializable
    public boolean glowingEffect = true;  // 发光效果

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig enrageParticle = new ParticleConfig();

    {
        // 默认狂暴粒子：愤怒粒子
        enrageParticle.type = Particle.ANGRY_VILLAGER;
        enrageParticle.amount = 10;
        enrageParticle.deltaX = 0.5;
        enrageParticle.deltaY = 0.8;
        enrageParticle.deltaZ = 0.5;
        enrageParticle.speed = 0;
    }

    @Serializable
    public String enrageSound = "ENTITY_WARDEN_ANGRY";

    @Override
    public void onHurt(IMob mob, EntityDamageEvent event) {
        LivingEntity entity = mob.getEntity();
        UUID mobId = entity.getUniqueId();

        // 如果设置为只触发一次且已经触发过，跳过
        if (triggerOnce && enragedMobs.contains(mobId)) {
            return;
        }

        // 计算伤害后的血量比例
        double currentHealth = entity.getHealth();
        double damage = event.getFinalDamage();
        double healthAfterDamage = currentHealth - damage;

        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double maxHealth = maxHealthAttr.getValue();
        double healthPercentAfterDamage = healthAfterDamage / maxHealth;

        // 检查是否达到阈值
        if (healthPercentAfterDamage > healthThreshold) {
            return;
        }

        // 检查当前血量是否已经低于阈值（防止重复触发）
        double currentHealthPercent = currentHealth / maxHealth;
        if (currentHealthPercent <= healthThreshold && triggerOnce) {
            return;
        }

        // 触发狂暴
        triggerEnrage(mob);
    }

    /**
     * 触发狂暴状态
     */
    private void triggerEnrage(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        UUID mobId = entity.getUniqueId();
        World world = entity.getWorld();
        Location location = entity.getLocation();

        // 标记为已狂暴
        enragedMobs.add(mobId);

        // 播放狂暴音效
        try {
            Sound sound = Sound.valueOf(enrageSound);
            world.playSound(location, sound, 1.5f, 0.8f);
        } catch (IllegalArgumentException ignored) {}

        // 狂暴粒子效果
        Utils.spawnParticle(enrageParticle, world, location.add(0, 1, 0));

        // 应用狂暴效果
        applyEnrageEffect(entity, enrageEffect1, enrageEffect1Duration, enrageEffect1Amplifier);
        applyEnrageEffect(entity, enrageEffect2, enrageEffect2Duration, enrageEffect2Amplifier);
        applyEnrageEffect(entity, enrageEffect3, enrageEffect3Duration, enrageEffect3Amplifier);

        // 发光效果
        if (glowingEffect) {
            entity.setGlowing(true);
        }
    }

    /**
     * 应用狂暴效果
     */
    private void applyEnrageEffect(LivingEntity entity, String effect, int duration, int amplifier) {
        if (effect == null || effect.isEmpty() || amplifier < 0) {
            return;
        }

        try {
            PotionEffectType effectType = Utils.parseEffect(effect, getName());
            if (effectType != null) {
                // -1 表示永久效果（使用很长的时间）
                int actualDuration = duration < 0 ? Integer.MAX_VALUE : duration;
                entity.addPotionEffect(new PotionEffect(effectType, actualDuration, amplifier, false, true, true));
            }
        } catch (Exception ignored) {}
    }

    /**
     * 检查怪物是否已狂暴
     */
    public static boolean isEnraged(UUID mobId) {
        return enragedMobs.contains(mobId);
    }

    /**
     * 清理已死亡怪物的记录
     */
    public static void cleanupDeadMob(UUID mobId) {
        enragedMobs.remove(mobId);
    }

    @Override
    public String getName() {
        return "Enrage";
    }
}
