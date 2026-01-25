package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityShadowStep - 暗影突袭
 *
 * 主动隐身并加速移动，接近目标后解除隐身并造成额外伤害。
 */
public class AbilityShadowStep extends ActiveAbility implements AbilityAttack {

    // 追踪正在潜行的怪物
    private static final Map<UUID, StealthState> stealthMobs = new ConcurrentHashMap<>();

    // === 配置参数 ===

    @Serializable
    public int stealthDuration = 100;  // 潜行持续时间 5秒

    @Serializable
    public int speedAmplifier = 2;  // 速度等级

    @Serializable
    public double bonusDamageMultiplier = 2.0;  // 额外伤害倍率

    @Serializable
    public boolean revealOnAttack = true;  // 攻击后解除潜行

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig stealthParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig revealParticle = new ParticleConfig();

    {
        // 默认潜行粒子：烟雾
        stealthParticle.type = Particle.SMOKE;
        stealthParticle.amount = 10;
        stealthParticle.deltaX = 0.2;
        stealthParticle.deltaY = 0.5;
        stealthParticle.deltaZ = 0.2;
        stealthParticle.speed = 0.02;

        // 默认现身粒子：爆发烟雾
        revealParticle.type = Particle.CAMPFIRE_COSY_SMOKE;
        revealParticle.amount = 30;
        revealParticle.deltaX = 0.5;
        revealParticle.deltaY = 1.0;
        revealParticle.deltaZ = 0.5;
        revealParticle.speed = 0.1;
    }

    @Serializable
    public String stealthSound = "ENTITY_ILLUSIONER_MIRROR_MOVE";

    @Serializable
    public String revealSound = "ENTITY_ILLUSIONER_PREPARE_BLINDNESS";

    @Override
    public void active(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        UUID mobId = entity.getUniqueId();

        // 如果已经在潜行中，不重复触发
        if (stealthMobs.containsKey(mobId)) {
            return;
        }

        World world = entity.getWorld();

        // 播放进入潜行音效
        try {
            Sound sound = Sound.valueOf(stealthSound);
            world.playSound(entity.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}

        // 应用潜行效果
        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, stealthDuration, 0, false, false));
        if (speedAmplifier >= 0) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, stealthDuration, speedAmplifier, false, false));
        }

        // 记录潜行状态
        StealthState state = new StealthState(bonusDamageMultiplier, System.currentTimeMillis() + stealthDuration * 50L);
        stealthMobs.put(mobId, state);

        // 潜行期间的粒子效果
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (entity.isDead() || !stealthMobs.containsKey(mobId) || ticksElapsed >= stealthDuration) {
                    revealMob(iMob);
                    this.cancel();
                    return;
                }

                // 每 5 tick 产生一次粒子
                if (ticksElapsed % 5 == 0) {
                    Utils.spawnParticle(stealthParticle, world, entity.getLocation().add(0, 0.5, 0));
                }

                ticksElapsed++;
            }
        }.runTaskTimer(InfPlugin.plugin, 0L, 1L);
    }

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        UUID mobId = mob.getEntity().getUniqueId();
        StealthState state = stealthMobs.get(mobId);

        if (state != null && revealOnAttack) {
            // 攻击时造成额外伤害（通过修改怪物伤害实现）
            // 注意：实际伤害增加需要在 Events 处理，这里只提供标记
            // 触发现身
            revealMob(mob);
        }
    }

    /**
     * 获取潜行状态的伤害倍率
     */
    public static double getStealthDamageMultiplier(UUID mobId) {
        StealthState state = stealthMobs.get(mobId);
        if (state != null && System.currentTimeMillis() < state.expiresAt) {
            return state.damageMultiplier;
        }
        return 1.0;
    }

    /**
     * 检查怪物是否在潜行状态
     */
    public static boolean isStealthed(UUID mobId) {
        StealthState state = stealthMobs.get(mobId);
        return state != null && System.currentTimeMillis() < state.expiresAt;
    }

    private void revealMob(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        UUID mobId = entity.getUniqueId();

        if (!stealthMobs.containsKey(mobId)) {
            return;
        }

        stealthMobs.remove(mobId);

        // 移除隐身效果
        entity.removePotionEffect(PotionEffectType.INVISIBILITY);

        World world = entity.getWorld();

        // 现身粒子效果
        Utils.spawnParticle(revealParticle, world, entity.getLocation().add(0, 1, 0));

        // 现身音效
        try {
            Sound sound = Sound.valueOf(revealSound);
            world.playSound(entity.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public String getName() {
        return "ShadowStep";
    }

    /**
     * 潜行状态数据
     */
    private static class StealthState {
        final double damageMultiplier;
        final long expiresAt;

        StealthState(double damageMultiplier, long expiresAt) {
            this.damageMultiplier = damageMultiplier;
            this.expiresAt = expiresAt;
        }
    }
}
