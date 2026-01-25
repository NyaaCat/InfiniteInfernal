package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityDeath;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityPoisonTrail - 毒雾尾迹
 *
 * 移动时留下毒雾，或死亡时释放毒云。
 */
public class AbilityPoisonTrail extends AbilityPassive implements AbilitySpawn, AbilityDeath {

    // 追踪活跃的毒雾任务
    private static final Map<UUID, BukkitRunnable> trailTasks = new ConcurrentHashMap<>();

    // 追踪所有活跃的毒云区域
    private static final List<PoisonCloud> activeClouds = new ArrayList<>();

    // === 移动尾迹配置 ===

    @Serializable
    public boolean trailEnabled = true;  // 启用移动尾迹

    @Serializable
    public int trailInterval = 20;  // 每20tick留下一个

    @Serializable
    public double trailRadius = 1.5;  // 毒雾半径

    @Serializable
    public int trailDuration = 60;  // 毒雾持续时间

    @Serializable
    public String trailPoisonEffect = "POISON";  // 毒雾效果

    @Serializable
    public int trailPoisonDuration = 40;  // 中毒时间

    @Serializable
    public int trailPoisonAmplifier = 0;  // 中毒等级

    // === 死亡毒云配置 ===

    @Serializable
    public boolean deathCloudEnabled = true;  // 启用死亡毒云

    @Serializable
    public double deathCloudRadius = 5.0;  // 死亡毒云半径

    @Serializable
    public int deathCloudDuration = 100;  // 毒云持续时间

    @Serializable
    public double deathCloudDamage = 2.0;  // 每秒伤害

    @Serializable
    public String deathCloudEffect = "POISON";  // 死亡毒云效果

    @Serializable
    public int deathCloudEffectDuration = 60;

    @Serializable
    public int deathCloudEffectAmplifier = 1;

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig trailParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig deathParticle = new ParticleConfig();

    {
        // 默认尾迹粒子：龙息
        trailParticle.type = Particle.DRAGON_BREATH;
        trailParticle.amount = 10;
        trailParticle.deltaX = 0.3;
        trailParticle.deltaY = 0.2;
        trailParticle.deltaZ = 0.3;
        trailParticle.speed = 0.02;

        // 默认死亡粒子：篝火烟
        deathParticle.type = Particle.CAMPFIRE_COSY_SMOKE;
        deathParticle.amount = 30;
        deathParticle.deltaX = 1.0;
        deathParticle.deltaY = 0.5;
        deathParticle.deltaZ = 1.0;
        deathParticle.speed = 0.05;
    }

    @Serializable
    public String trailSound = "";  // 尾迹音效（可选）

    @Serializable
    public String deathSound = "ENTITY_PUFFER_FISH_BLOW_UP";

    @Override
    public void onSpawn(IMob iMob) {
        if (!trailEnabled) return;

        LivingEntity entity = iMob.getEntity();
        UUID mobId = entity.getUniqueId();

        // 如果已有任务，先取消
        BukkitRunnable existingTask = trailTasks.remove(mobId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // 创建尾迹任务
        BukkitRunnable trailTask = new BukkitRunnable() {
            Location lastLocation = entity.getLocation();

            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    trailTasks.remove(mobId);
                    this.cancel();
                    return;
                }

                Location currentLocation = entity.getLocation();

                // 检查是否移动了足够距离
                if (currentLocation.distance(lastLocation) > 0.5) {
                    createPoisonCloud(lastLocation, trailRadius, trailDuration,
                        trailPoisonEffect, trailPoisonDuration, trailPoisonAmplifier,
                        0, trailParticle, trailSound);
                    lastLocation = currentLocation.clone();
                }
            }
        };

        trailTasks.put(mobId, trailTask);
        trailTask.runTaskTimer(InfPlugin.plugin, trailInterval, trailInterval);
    }

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        LivingEntity entity = iMob.getEntity();
        UUID mobId = entity.getUniqueId();

        // 取消尾迹任务
        BukkitRunnable task = trailTasks.remove(mobId);
        if (task != null) {
            task.cancel();
        }

        // 死亡毒云
        if (deathCloudEnabled) {
            Location deathLocation = entity.getLocation();
            createPoisonCloud(deathLocation, deathCloudRadius, deathCloudDuration,
                deathCloudEffect, deathCloudEffectDuration, deathCloudEffectAmplifier,
                deathCloudDamage, deathParticle, deathSound);
        }
    }

    /**
     * 创建毒云区域
     */
    private void createPoisonCloud(Location center, double radius, int duration,
                                   String effect, int effectDuration, int effectAmplifier,
                                   double damage, ParticleConfig particle, String sound) {
        World world = center.getWorld();
        if (world == null) return;

        // 播放音效
        if (sound != null && !sound.isEmpty()) {
            try {
                Sound s = Sound.valueOf(sound);
                world.playSound(center, s, 0.5f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }

        // 创建毒云对象
        PoisonCloud cloud = new PoisonCloud(center, radius, effect, effectDuration,
            effectAmplifier, damage, particle, System.currentTimeMillis() + duration * 50L);

        synchronized (activeClouds) {
            activeClouds.add(cloud);
        }

        // 毒云持续效果任务
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= duration) {
                    synchronized (activeClouds) {
                        activeClouds.remove(cloud);
                    }
                    this.cancel();
                    return;
                }

                // 每秒产生粒子和施加效果
                if (ticksElapsed % 20 == 0) {
                    // 粒子效果
                    spawnCloudParticles(center, radius, particle, world);

                    // 对范围内玩家施加效果
                    for (Player player : world.getPlayers()) {
                        if (!Utils.validGamemode(player)) continue;
                        if (player.getLocation().distance(center) > radius) continue;

                        // 施加毒效果
                        if (effect != null && !effect.isEmpty() && effectAmplifier >= 0) {
                            try {
                                Utils.doEffect(effect, player, effectDuration, effectAmplifier, getName());
                            } catch (Exception ignored) {}
                        }

                        // 直接伤害
                        if (damage > 0) {
                            player.damage(damage);
                        }
                    }
                }

                // 每 5 tick 产生少量粒子
                if (ticksElapsed % 5 == 0) {
                    Utils.spawnParticle(particle, world, center.clone().add(
                        Utils.random(-radius, radius) * 0.5,
                        Utils.random(0, 1),
                        Utils.random(-radius, radius) * 0.5
                    ));
                }

                ticksElapsed++;
            }
        }.runTaskTimer(InfPlugin.plugin, 0L, 1L);
    }

    /**
     * 产生毒云粒子效果
     */
    private void spawnCloudParticles(Location center, double radius, ParticleConfig particle, World world) {
        // 在区域内随机产生多个粒子
        for (int i = 0; i < 8; i++) {
            Location particleLoc = center.clone().add(
                Utils.random(-radius, radius),
                Utils.random(0, 1.5),
                Utils.random(-radius, radius)
            );
            Utils.spawnParticle(particle, world, particleLoc);
        }
    }

    /**
     * 检查位置是否在毒云中
     */
    public static boolean isInPoisonCloud(Location location) {
        long currentTime = System.currentTimeMillis();
        synchronized (activeClouds) {
            Iterator<PoisonCloud> it = activeClouds.iterator();
            while (it.hasNext()) {
                PoisonCloud cloud = it.next();
                if (currentTime > cloud.expiresAt) {
                    it.remove();
                    continue;
                }
                if (cloud.center.getWorld() != null &&
                    cloud.center.getWorld().equals(location.getWorld()) &&
                    cloud.center.distance(location) <= cloud.radius) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "PoisonTrail";
    }

    /**
     * 毒云数据类
     */
    private static class PoisonCloud {
        final Location center;
        final double radius;
        final String effect;
        final int effectDuration;
        final int effectAmplifier;
        final double damage;
        final ParticleConfig particle;
        final long expiresAt;

        PoisonCloud(Location center, double radius, String effect, int effectDuration,
                    int effectAmplifier, double damage, ParticleConfig particle, long expiresAt) {
            this.center = center.clone();
            this.radius = radius;
            this.effect = effect;
            this.effectDuration = effectDuration;
            this.effectAmplifier = effectAmplifier;
            this.damage = damage;
            this.particle = particle;
            this.expiresAt = expiresAt;
        }
    }
}
