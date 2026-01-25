package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * AbilityWebShot - 蛛网射击
 *
 * 发射蛛网投射物黏住玩家。与 AbilityStuck 不同，这是投射物形式，需要瞄准。
 */
public class AbilityWebShot extends ActiveAbility {

    private static final String WEB_SHOT_KEY = "inf_web_shot";

    // 追踪放置的临时蛛网
    private static final List<Block> temporaryWebs = new ArrayList<>();

    // === 配置参数 ===

    @Serializable
    public double projectileSpeed = 1.5;  // 投射物速度

    @Serializable
    public int webDuration = 60;  // 蛛网持续时间 3秒

    @Serializable
    public int webRadius = 1;  // 蛛网范围（1=单格，2=3x3）

    @Serializable
    public boolean slowEffect = true;  // 是否额外施加减速

    @Serializable
    public int slowAmplifier = 2;  // 减速等级

    @Serializable
    public int slowDuration = 40;  // 减速时间

    @Serializable
    public int burstCount = 1;  // 连发数量

    @Serializable
    public int burstInterval = 5;  // 连发间隔

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig projectileParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig hitParticle = new ParticleConfig();

    {
        // 默认投射物粒子：白色粒子
        projectileParticle.type = Particle.ITEM_SNOWBALL;
        projectileParticle.amount = 3;
        projectileParticle.deltaX = 0.1;
        projectileParticle.deltaY = 0.1;
        projectileParticle.deltaZ = 0.1;
        projectileParticle.speed = 0;

        // 默认命中粒子：方块破碎
        hitParticle.type = Particle.BLOCK;
        hitParticle.amount = 30;
        hitParticle.deltaX = 0.5;
        hitParticle.deltaY = 0.5;
        hitParticle.deltaZ = 0.5;
        hitParticle.speed = 0.1;
        hitParticle.extraData = "COBWEB";
    }

    @Serializable
    public String shootSound = "ENTITY_SPIDER_AMBIENT";

    @Serializable
    public String hitSound = "BLOCK_COBWEB_PLACE";

    @Override
    public void active(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        LivingEntity target = iMob.getTarget();

        if (target == null) {
            // 没有目标时尝试获取附近玩家
            List<Player> nearbyPlayers = getNearbyPlayers(iMob, 30);
            if (!nearbyPlayers.isEmpty()) {
                target = Utils.randomPick(nearbyPlayers);
            }
        }

        if (target == null) return;

        final LivingEntity finalTarget = target;
        World world = entity.getWorld();

        // 播放发射音效
        try {
            Sound sound = Sound.valueOf(shootSound);
            world.playSound(entity.getLocation(), sound, 1.0f, 0.8f);
        } catch (IllegalArgumentException ignored) {}

        // 注册命中监听器
        WebShotListener listener = new WebShotListener();
        InfPlugin.plugin.getServer().getPluginManager().registerEvents(listener, InfPlugin.plugin);

        // 发射投射物（支持连发）
        for (int i = 0; i < burstCount; i++) {
            final int delay = i * burstInterval;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isDead()) return;
                    launchWebProjectile(entity, finalTarget, listener);
                }
            }.runTaskLater(InfPlugin.plugin, delay);
        }

        // 延迟注销监听器
        new BukkitRunnable() {
            @Override
            public void run() {
                HandlerList.unregisterAll(listener);
            }
        }.runTaskLater(InfPlugin.plugin, 200);  // 10秒后注销
    }

    /**
     * 发射蛛网投射物
     */
    private void launchWebProjectile(LivingEntity shooter, LivingEntity target, WebShotListener listener) {
        // 计算发射方向
        Vector direction = Utils.unitDirectionVector(
            shooter.getEyeLocation().toVector(),
            target.getEyeLocation().toVector()
        ).multiply(projectileSpeed);

        // 使用雪球作为投射物载体
        Snowball projectile = shooter.launchProjectile(Snowball.class, direction);
        projectile.setMetadata(WEB_SHOT_KEY, new FixedMetadataValue(InfPlugin.plugin, this));

        listener.addProjectile(projectile);

        // 投射物粒子轨迹
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (projectile.isDead() || !projectile.isValid() || ticks > 100) {
                    this.cancel();
                    return;
                }

                World world = projectile.getWorld();
                Utils.spawnParticle(projectileParticle, world, projectile.getLocation());
                ticks++;
            }
        }.runTaskTimer(InfPlugin.plugin, 0L, 1L);
    }

    /**
     * 在指定位置放置临时蛛网
     */
    private void placeTemporaryWeb(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        List<Block> placedWebs = new ArrayList<>();

        // 根据 webRadius 放置蛛网
        int radius = webRadius - 1;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = location.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                    block.setType(Material.COBWEB);
                    placedWebs.add(block);
                    temporaryWebs.add(block);
                }
            }
        }

        // 定时移除蛛网
        if (!placedWebs.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Block block : placedWebs) {
                        if (block.getType() == Material.COBWEB) {
                            block.setType(Material.AIR);
                        }
                        temporaryWebs.remove(block);
                    }
                }
            }.runTaskLater(InfPlugin.plugin, webDuration);
        }
    }

    @Override
    public String getName() {
        return "WebShot";
    }

    /**
     * 投射物命中监听器
     */
    private class WebShotListener implements Listener {
        private final List<Snowball> projectiles = new ArrayList<>();

        void addProjectile(Snowball projectile) {
            projectiles.add(projectile);
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void onHit(ProjectileHitEvent event) {
            if (!(event.getEntity() instanceof Snowball)) return;

            Snowball snowball = (Snowball) event.getEntity();
            if (!projectiles.contains(snowball)) return;
            if (!snowball.hasMetadata(WEB_SHOT_KEY)) return;

            projectiles.remove(snowball);

            Location hitLocation = snowball.getLocation();
            World world = hitLocation.getWorld();

            if (world == null) return;

            // 播放命中音效
            try {
                Sound sound = Sound.valueOf(hitSound);
                world.playSound(hitLocation, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}

            // 命中粒子效果
            Utils.spawnParticle(hitParticle, world, hitLocation);

            // 放置临时蛛网
            placeTemporaryWeb(hitLocation);

            // 对命中的实体施加减速效果
            if (event.getHitEntity() instanceof LivingEntity && slowEffect && slowAmplifier >= 0) {
                LivingEntity hitEntity = (LivingEntity) event.getHitEntity();
                try {
                    Utils.doEffect("SLOWNESS", hitEntity, slowDuration, slowAmplifier, getName());
                } catch (Exception ignored) {}
            }
        }
    }
}
