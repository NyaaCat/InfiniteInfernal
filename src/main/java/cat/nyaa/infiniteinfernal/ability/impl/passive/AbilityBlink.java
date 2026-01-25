package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.ability.AbilityHurt;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityBlink - 闪现闪避
 *
 * 受到伤害时有概率瞬移到附近位置，躲避后续攻击。
 * 与 AbilityTeleport 不同，这是被动触发的闪避机制。
 */
public class AbilityBlink extends AbilityPassive implements AbilityHurt {

    // 冷却追踪
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // === 配置参数 ===

    @Serializable
    public double blinkChance = 0.3;  // 触发概率 30%

    @Serializable
    public double blinkDistance = 5.0;  // 传送距离

    @Serializable
    public boolean blinkBehindAttacker = true;  // 传送到攻击者身后

    @Serializable
    public int cooldownTicks = 60;  // 冷却时间 3秒

    @Serializable
    public String postBlinkEffect = "SPEED";  // 传送后获得效果

    @Serializable
    public int postBlinkDuration = 40;  // 效果持续时间

    @Serializable
    public int postBlinkAmplifier = 1;  // 效果等级

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig vanishParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig appearParticle = new ParticleConfig();

    {
        // 默认消失粒子：烟雾
        vanishParticle.type = Particle.SMOKE;
        vanishParticle.amount = 30;
        vanishParticle.deltaX = 0.3;
        vanishParticle.deltaY = 0.5;
        vanishParticle.deltaZ = 0.3;
        vanishParticle.speed = 0.05;

        // 默认出现粒子：末影粒子
        appearParticle.type = Particle.PORTAL;
        appearParticle.amount = 50;
        appearParticle.deltaX = 0.5;
        appearParticle.deltaY = 1.0;
        appearParticle.deltaZ = 0.5;
        appearParticle.speed = 0.1;
    }

    @Serializable
    public String blinkSound = "ENTITY_ENDERMAN_TELEPORT";

    @Override
    public void onHurt(IMob mob, EntityDamageEvent event) {
        // 检查冷却
        UUID mobId = mob.getEntity().getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastBlink = cooldowns.get(mobId);
        long cooldownMs = cooldownTicks * 50L;  // tick to ms

        if (lastBlink != null && currentTime - lastBlink < cooldownMs) {
            return;
        }

        // 概率判定
        if (!Utils.possibility(blinkChance)) {
            return;
        }

        LivingEntity entity = mob.getEntity();
        if (entity.isInsideVehicle()) {
            return;
        }

        Location originalLocation = entity.getLocation();
        Location targetLocation = null;

        // 尝试传送到攻击者身后
        if (blinkBehindAttacker && event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();

            // 处理投射物
            if (damager instanceof Projectile) {
                Object shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Entity) {
                    damager = (Entity) shooter;
                }
            }

            if (damager instanceof LivingEntity) {
                targetLocation = calculateBehindLocation((LivingEntity) damager);
            }
        }

        // 如果无法传送到攻击者身后，随机传送
        if (targetLocation == null) {
            targetLocation = Utils.randomNonNullLocation(originalLocation, blinkDistance * 0.5, blinkDistance);
        }

        // 调用传送事件
        EntityTeleportEvent teleportEvent = new EntityTeleportEvent(entity, originalLocation, targetLocation);
        Bukkit.getPluginManager().callEvent(teleportEvent);
        if (teleportEvent.isCancelled()) {
            return;
        }

        Location finalLocation = teleportEvent.getTo();
        if (finalLocation == null) {
            return;
        }

        // 执行传送
        World world = originalLocation.getWorld();
        if (world != null) {
            // 原位置粒子效果
            Utils.spawnParticle(vanishParticle, world, originalLocation.add(0, 1, 0));

            // 播放音效
            try {
                Sound sound = Sound.valueOf(blinkSound);
                world.playSound(originalLocation, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }

        entity.teleport(finalLocation);

        // 新位置粒子效果
        World newWorld = finalLocation.getWorld();
        if (newWorld != null) {
            Utils.spawnParticle(appearParticle, newWorld, finalLocation.add(0, 1, 0));
        }

        // 应用传送后效果
        if (postBlinkEffect != null && !postBlinkEffect.isEmpty() && postBlinkAmplifier >= 0) {
            try {
                Utils.doEffect(postBlinkEffect, entity, postBlinkDuration, postBlinkAmplifier, getName());
            } catch (Exception ignored) {}
        }

        // 记录冷却
        cooldowns.put(mobId, currentTime);
    }

    /**
     * 计算攻击者身后的位置
     */
    private Location calculateBehindLocation(LivingEntity attacker) {
        Location attackerLoc = attacker.getLocation();
        Vector direction = attackerLoc.getDirection().normalize();

        // 攻击者身后
        Vector behind = direction.multiply(-blinkDistance);
        Location targetLocation = attackerLoc.clone().add(behind);

        return Utils.findValidSpawnLocationInY(targetLocation);
    }

    @Override
    public String getName() {
        return "Blink";
    }
}
