package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityLeapSlam - 跃击重踏
 *
 * 跳跃扑向目标，落地时造成范围伤害和击退。
 */
public class AbilityLeapSlam extends ActiveAbility {

    // 追踪正在跳跃的怪物（用于免疫摔落伤害）
    private static final Map<UUID, Long> leapingMobs = new ConcurrentHashMap<>();

    // === 配置参数 ===

    @Serializable
    public double leapRange = 15.0;  // 最大跳跃距离

    @Serializable
    public double leapHeight = 1.5;  // 跳跃高度倍率

    @Serializable
    public double slamRadius = 4.0;  // 落地伤害半径

    @Serializable
    public double slamDamageMultiplier = 1.5;  // 落地伤害倍率

    @Serializable
    public double slamKnockback = 1.5;  // 击退力度

    @Serializable
    public int slamSlowDuration = 20;  // 落地减速时间

    @Serializable
    public int slamSlowAmplifier = 1;  // 落地减速等级

    @Serializable
    public boolean fallDamageImmune = true;  // 免疫摔落伤害

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig leapParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig slamParticle = new ParticleConfig();

    {
        // 默认起跳粒子：云雾
        leapParticle.type = Particle.CLOUD;
        leapParticle.amount = 20;
        leapParticle.deltaX = 0.3;
        leapParticle.deltaY = 0.1;
        leapParticle.deltaZ = 0.3;
        leapParticle.speed = 0.05;

        // 默认落地粒子：爆炸效果
        slamParticle.type = Particle.EXPLOSION;
        slamParticle.amount = 5;
        slamParticle.deltaX = 0.5;
        slamParticle.deltaY = 0.2;
        slamParticle.deltaZ = 0.5;
        slamParticle.speed = 0;
    }

    @Serializable
    public String leapSound = "ENTITY_RAVAGER_ATTACK";

    @Serializable
    public String slamSound = "ENTITY_GENERIC_EXPLODE";

    @Override
    public void active(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        LivingEntity target = iMob.getTarget();

        // 如果没有目标，尝试获取附近玩家
        if (target == null) {
            List<Player> nearbyPlayers = getNearbyPlayers(iMob, leapRange);
            if (!nearbyPlayers.isEmpty()) {
                target = Utils.randomPick(nearbyPlayers);
            }
        }

        if (target == null) return;

        Location startLoc = entity.getLocation();
        Location targetLoc = target.getLocation();
        World world = startLoc.getWorld();

        if (world == null) return;

        // 检查距离
        double distance = startLoc.distance(targetLoc);
        if (distance > leapRange) {
            // 目标太远，向目标方向跳跃最大距离
            Vector direction = Utils.unitDirectionVector(startLoc.toVector(), targetLoc.toVector());
            targetLoc = startLoc.clone().add(direction.multiply(leapRange));
        }

        // 播放起跳音效
        try {
            Sound sound = Sound.valueOf(leapSound);
            world.playSound(startLoc, sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}

        // 起跳粒子效果
        Utils.spawnParticle(leapParticle, world, startLoc);

        // 计算跳跃向量
        Vector leapVector = calculateLeapVector(startLoc, targetLoc, leapHeight);

        // 标记为跳跃中（用于免疫摔落伤害）
        UUID mobId = entity.getUniqueId();
        if (fallDamageImmune) {
            leapingMobs.put(mobId, System.currentTimeMillis() + 5000);  // 5秒超时
        }

        // 应用跳跃向量
        entity.setVelocity(leapVector);

        // 存储数据用于落地检测
        final double baseDamage = iMob.getDamage();
        final Location finalTargetLoc = targetLoc;

        // 监测落地
        new BukkitRunnable() {
            int ticks = 0;
            boolean wasInAir = false;

            @Override
            public void run() {
                if (entity.isDead() || ticks > 100) {  // 5秒超时
                    leapingMobs.remove(mobId);
                    this.cancel();
                    return;
                }

                boolean isOnGround = entity.isOnGround();

                // 检测从空中落地
                if (wasInAir && isOnGround) {
                    performSlam(entity, baseDamage, iMob);
                    leapingMobs.remove(mobId);
                    this.cancel();
                    return;
                }

                // 在空中时产生粒子轨迹
                if (!isOnGround) {
                    wasInAir = true;
                    if (ticks % 2 == 0) {
                        Utils.spawnParticle(leapParticle, world, entity.getLocation());
                    }
                }

                ticks++;
            }
        }.runTaskTimer(InfPlugin.plugin, 1L, 1L);
    }

    /**
     * 计算跳跃向量
     */
    private Vector calculateLeapVector(Location from, Location to, double heightMultiplier) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();

        // 水平速度
        double horizontalSpeed = Math.min(distance / 20.0, 1.5);  // 限制水平速度

        // 垂直速度（根据距离和高度倍率）
        double verticalSpeed = Math.sqrt(distance * 0.1) * heightMultiplier;
        verticalSpeed = Math.min(verticalSpeed, 1.5);  // 限制垂直速度

        direction.setY(0).normalize().multiply(horizontalSpeed);
        direction.setY(verticalSpeed);

        return direction;
    }

    /**
     * 执行落地重击
     */
    private void performSlam(LivingEntity entity, double baseDamage, IMob iMob) {
        Location slamLocation = entity.getLocation();
        World world = slamLocation.getWorld();

        if (world == null) return;

        // 播放落地音效
        try {
            Sound sound = Sound.valueOf(slamSound);
            world.playSound(slamLocation, sound, 1.5f, 0.8f);
        } catch (IllegalArgumentException ignored) {}

        // 落地粒子效果
        Utils.spawnParticle(slamParticle, world, slamLocation);

        // 在落地点周围产生地面粒子
        List<Location> circleLocations = Utils.getRoundLocations(slamLocation, slamRadius);
        ParticleConfig groundParticle = new ParticleConfig();
        groundParticle.type = Particle.BLOCK;
        groundParticle.amount = 5;
        groundParticle.speed = 0.1;
        groundParticle.extraData = "DIRT";

        for (int i = 0; i < circleLocations.size(); i += Math.max(1, circleLocations.size() / 12)) {
            Utils.spawnParticle(groundParticle, world, circleLocations.get(i));
        }

        // 对范围内敌人造成伤害
        List<LivingEntity> nearbyEntities = getNearbyEntities(iMob, slamRadius);

        double slamDamage = baseDamage * slamDamageMultiplier;

        for (LivingEntity target : nearbyEntities) {
            if (target.equals(entity)) continue;

            // 计算距离衰减
            double distance = target.getLocation().distance(slamLocation);
            double damageMultiplier = 1.0 - (distance / slamRadius) * 0.5;  // 边缘伤害50%
            damageMultiplier = Math.max(0.5, damageMultiplier);

            double finalDamage = slamDamage * damageMultiplier;

            // 造成伤害
            target.damage(finalDamage, entity);

            // 击退效果
            Vector knockback = Utils.unitDirectionVector(slamLocation.toVector(), target.getLocation().toVector());
            knockback.setY(0.3).multiply(slamKnockback);
            target.setVelocity(target.getVelocity().add(knockback));

            // 减速效果
            if (slamSlowDuration > 0 && slamSlowAmplifier >= 0) {
                try {
                    Utils.doEffect("SLOWNESS", target, slamSlowDuration, slamSlowAmplifier, getName());
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 检查怪物是否正在跳跃（用于免疫摔落伤害）
     */
    public static boolean isLeaping(UUID mobId) {
        Long expiresAt = leapingMobs.get(mobId);
        if (expiresAt == null) return false;
        if (System.currentTimeMillis() > expiresAt) {
            leapingMobs.remove(mobId);
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "LeapSlam";
    }
}
