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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * AbilityMirrorImage - 镜像分身
 *
 * 创建假分身迷惑玩家。与 AbilityClone 不同，这些分身是假的，被击中立即消失。
 */
public class AbilityMirrorImage extends ActiveAbility {

    // 追踪所有镜像分身
    private static final Set<UUID> mirrorImages = Collections.synchronizedSet(new HashSet<>());

    // === 配置参数 ===

    @Serializable
    public int imageCount = 3;  // 分身数量

    @Serializable
    public int imageDuration = 100;  // 分身存在时间

    @Serializable
    public double imageHealth = 1.0;  // 分身血量（极低）

    @Serializable
    public boolean ownerInvisible = true;  // 本体是否隐身

    @Serializable
    public int ownerInvisibleDuration = 40;  // 本体隐身时间

    @Serializable
    public double spawnRadius = 3.0;  // 分身生成范围

    // === 粒子效果 ===
    @Serializable
    public ParticleConfig spawnParticle = new ParticleConfig();

    @Serializable
    public ParticleConfig deathParticle = new ParticleConfig();

    {
        // 默认生成粒子：魔法粒子
        spawnParticle.type = Particle.WITCH;
        spawnParticle.amount = 30;
        spawnParticle.deltaX = 0.3;
        spawnParticle.deltaY = 0.8;
        spawnParticle.deltaZ = 0.3;
        spawnParticle.speed = 0.1;

        // 默认消失粒子：云雾
        deathParticle.type = Particle.CLOUD;
        deathParticle.amount = 20;
        deathParticle.deltaX = 0.3;
        deathParticle.deltaY = 0.5;
        deathParticle.deltaZ = 0.3;
        deathParticle.speed = 0.05;
    }

    @Serializable
    public String spawnSound = "ENTITY_ILLUSIONER_CAST_SPELL";

    @Serializable
    public String deathSound = "ENTITY_ILLUSIONER_MIRROR_MOVE";

    @Override
    public void active(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        Location center = entity.getLocation();
        World world = center.getWorld();

        if (world == null) return;

        // 播放生成音效
        try {
            Sound sound = Sound.valueOf(spawnSound);
            world.playSound(center, sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}

        // 如果本体需要隐身
        if (ownerInvisible && ownerInvisibleDuration > 0) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ownerInvisibleDuration, 0, false, false));
        }

        // 创建分身
        List<LivingEntity> images = new ArrayList<>();
        for (int i = 0; i < imageCount; i++) {
            Location spawnLoc = Utils.randomNonNullLocation(center, 1, spawnRadius);
            if (spawnLoc == null) {
                spawnLoc = center.clone().add(Utils.random(-spawnRadius, spawnRadius), 0, Utils.random(-spawnRadius, spawnRadius));
            }

            LivingEntity image = spawnMirrorImage(entity, spawnLoc);
            if (image != null) {
                images.add(image);

                // 生成粒子效果
                Utils.spawnParticle(spawnParticle, world, spawnLoc.add(0, 1, 0));
            }
        }

        // 注册事件监听器处理分身受伤
        MirrorImageListener listener = new MirrorImageListener(images);
        InfPlugin.plugin.getServer().getPluginManager().registerEvents(listener, InfPlugin.plugin);

        // 定时移除分身
        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity image : images) {
                    if (image != null && !image.isDead()) {
                        destroyMirrorImage(image, world);
                    }
                }
                HandlerList.unregisterAll(listener);
            }
        }.runTaskLater(InfPlugin.plugin, imageDuration);
    }

    /**
     * 生成镜像分身
     */
    private LivingEntity spawnMirrorImage(LivingEntity original, Location location) {
        World world = location.getWorld();
        if (world == null) return null;

        try {
            // 生成相同类型的实体
            LivingEntity image = (LivingEntity) world.spawnEntity(location, original.getType());

            // 标记为镜像分身
            mirrorImages.add(image.getUniqueId());
            image.addScoreboardTag("inf_mirror_image");

            // 设置极低血量
            AttributeInstance maxHealthAttr = image.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(imageHealth);
                image.setHealth(imageHealth);
            }

            // 复制外观（如果有装备）
            if (original.getEquipment() != null && image.getEquipment() != null) {
                image.getEquipment().setArmorContents(original.getEquipment().getArmorContents());
                image.getEquipment().setItemInMainHand(original.getEquipment().getItemInMainHand());
                image.getEquipment().setItemInOffHand(original.getEquipment().getItemInOffHand());

                // 分身不掉落装备
                image.getEquipment().setHelmetDropChance(0);
                image.getEquipment().setChestplateDropChance(0);
                image.getEquipment().setLeggingsDropChance(0);
                image.getEquipment().setBootsDropChance(0);
                image.getEquipment().setItemInMainHandDropChance(0);
                image.getEquipment().setItemInOffHandDropChance(0);
            }

            // 复制自定义名称
            if (original.getCustomName() != null) {
                image.setCustomName(original.getCustomName());
                image.setCustomNameVisible(original.isCustomNameVisible());
            }

            // 分身不掉落经验和物品
            if (image instanceof Mob) {
                ((Mob) image).setTarget(original instanceof Mob ? ((Mob) original).getTarget() : null);
            }

            // 设置 AI 目标（让分身也会追击玩家）
            image.setAI(true);

            return image;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 销毁镜像分身
     */
    private void destroyMirrorImage(LivingEntity image, World world) {
        if (image == null || image.isDead()) return;

        mirrorImages.remove(image.getUniqueId());

        Location loc = image.getLocation();

        // 播放消失粒子
        Utils.spawnParticle(deathParticle, world, loc.add(0, 1, 0));

        // 播放消失音效
        try {
            Sound sound = Sound.valueOf(deathSound);
            world.playSound(loc, sound, 0.7f, 1.2f);
        } catch (IllegalArgumentException ignored) {}

        // 移除实体
        image.remove();
    }

    /**
     * 检查实体是否是镜像分身
     */
    public static boolean isMirrorImage(UUID entityId) {
        return mirrorImages.contains(entityId);
    }

    @Override
    public String getName() {
        return "MirrorImage";
    }

    /**
     * 分身事件监听器 - 处理分身受伤即死
     */
    private class MirrorImageListener implements Listener {
        private final List<LivingEntity> images;

        MirrorImageListener(List<LivingEntity> images) {
            this.images = images;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof LivingEntity)) return;

            LivingEntity entity = (LivingEntity) event.getEntity();
            if (images.contains(entity) && mirrorImages.contains(entity.getUniqueId())) {
                // 分身受到任何伤害立即死亡
                event.setCancelled(true);
                destroyMirrorImage(entity, entity.getWorld());
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onDeath(EntityDeathEvent event) {
            LivingEntity entity = event.getEntity();
            if (images.contains(entity)) {
                mirrorImages.remove(entity.getUniqueId());
                // 分身不掉落任何东西
                event.setDroppedExp(0);
                event.getDrops().clear();
            }
        }
    }
}
