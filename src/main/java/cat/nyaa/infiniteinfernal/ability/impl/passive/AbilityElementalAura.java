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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbilityElementalAura - 元素光环
 *
 * 持续性光环，对附近玩家施加负面效果。
 * 不同于 AoePotion 的一次性效果，这是持续的被动光环。
 */
public class AbilityElementalAura extends AbilityPassive implements AbilitySpawn, AbilityDeath {

    // 追踪活跃的光环任务
    private static final Map<UUID, BukkitRunnable> auraTasks = new ConcurrentHashMap<>();

    // === 配置参数 ===

    @Serializable
    public double auraRadius = 5.0;  // 光环半径

    @Serializable
    public int auraInterval = 20;  // 效果间隔 1秒

    // 光环效果列表（使用简单的字符串数组配置）
    @Serializable
    public String auraEffect1 = "WITHER";
    @Serializable
    public int auraEffect1Duration = 40;
    @Serializable
    public int auraEffect1Amplifier = 0;

    @Serializable
    public String auraEffect2 = "";
    @Serializable
    public int auraEffect2Duration = 40;
    @Serializable
    public int auraEffect2Amplifier = 0;

    @Serializable
    public String auraEffect3 = "";
    @Serializable
    public int auraEffect3Duration = 40;
    @Serializable
    public int auraEffect3Amplifier = 0;

    @Serializable
    public double auraDamage = 0;  // 可选：直接伤害

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig auraParticle = new ParticleConfig();

    {
        // 默认光环粒子：火焰
        auraParticle.type = Particle.FLAME;
        auraParticle.amount = 5;
        auraParticle.deltaX = 0.1;
        auraParticle.deltaY = 0.1;
        auraParticle.deltaZ = 0.1;
        auraParticle.speed = 0.02;
    }

    @Serializable
    public String auraSound = "";  // 光环音效（可选）

    @Override
    public void onSpawn(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        UUID mobId = entity.getUniqueId();

        // 如果已有光环任务，先取消
        BukkitRunnable existingTask = auraTasks.remove(mobId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // 创建持续光环任务
        BukkitRunnable auraTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    auraTasks.remove(mobId);
                    this.cancel();
                    return;
                }

                applyAuraEffects(iMob);
            }
        };

        auraTasks.put(mobId, auraTask);
        auraTask.runTaskTimer(InfPlugin.plugin, 0L, auraInterval);
    }

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        // 怪物死亡时取消光环任务
        UUID mobId = iMob.getEntity().getUniqueId();
        BukkitRunnable task = auraTasks.remove(mobId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 应用光环效果到范围内玩家
     */
    private void applyAuraEffects(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        Location center = entity.getLocation();
        World world = center.getWorld();

        if (world == null) return;

        // 获取范围内玩家
        List<Player> nearbyPlayers = getNearbyPlayers(iMob, auraRadius);

        if (nearbyPlayers.isEmpty()) {
            // 即使没有玩家也产生光环粒子效果
            spawnAuraParticles(center, world);
            return;
        }

        // 产生光环粒子
        spawnAuraParticles(center, world);

        // 播放光环音效
        if (auraSound != null && !auraSound.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(auraSound);
                world.playSound(center, sound, 0.5f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }

        // 对每个玩家施加效果
        for (Player player : nearbyPlayers) {
            // 施加药水效果
            applyEffect(player, auraEffect1, auraEffect1Duration, auraEffect1Amplifier);
            applyEffect(player, auraEffect2, auraEffect2Duration, auraEffect2Amplifier);
            applyEffect(player, auraEffect3, auraEffect3Duration, auraEffect3Amplifier);

            // 施加直接伤害
            if (auraDamage > 0) {
                player.damage(auraDamage, entity);
            }
        }
    }

    /**
     * 安全地施加药水效果
     */
    private void applyEffect(Player player, String effect, int duration, int amplifier) {
        if (effect == null || effect.isEmpty() || amplifier < 0) {
            return;
        }
        try {
            Utils.doEffect(effect, player, duration, amplifier, getName());
        } catch (Exception ignored) {}
    }

    /**
     * 在光环范围内产生粒子效果
     */
    private void spawnAuraParticles(Location center, World world) {
        // 生成环形粒子
        List<Location> circleLocations = Utils.getRoundLocations(center.clone().add(0, 0.5, 0), auraRadius);
        int step = Math.max(1, circleLocations.size() / 16);  // 每隔几个点产生一个粒子

        for (int i = 0; i < circleLocations.size(); i += step) {
            Utils.spawnParticle(auraParticle, world, circleLocations.get(i));
        }
    }

    @Override
    public String getName() {
        return "ElementalAura";
    }
}
