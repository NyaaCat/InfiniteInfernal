package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.metadata.LazyMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class AbilityProjectile extends ActiveAbility {
    public static String INF_PROJECTILE_KEY = "inf_projectile";
    @Serializable
    public int range = 30;
    @Serializable
    public double speed = 1;
    @Serializable
    public String projectile = "Arrow";
    @Serializable
    public double cone = 0;
    @Serializable
    public boolean gravity = true;
    @Serializable
    public int burstCount = 1;
    @Serializable
    public int burstInterval = 10;
    @Serializable
    public double damageAmplifier = 1.0d;


    @Override
    public void active(IMob iMob) {
        LivingEntity mobEntity = iMob.getEntity();
        int i = 0;
        AtomicInteger burstCounter = new AtomicInteger(0);
        class Task extends BukkitRunnable {
            @Override
            public void run() {
                if (iMob.getEntity().isDead()) return;
                if (burstCounter.getAndAdd(1) < burstCount) {
                    Vector direction = iMob.getEntity().getEyeLocation().getDirection();
                    direction.normalize().multiply(speed);
                    double length = direction.length();
                    if (Double.isInfinite(length) || Double.isNaN(length)) return;
                    launch(mobEntity, direction, damageAmplifier * iMob.getDamage());
                    new Task().runTaskLater(InfPlugin.plugin, burstInterval);
                }
            }
        }
        new Task().runTask(InfPlugin.plugin);
    }

    private Projectile launch(LivingEntity mobEntity, Vector vector, double damage) {
        vector = Utils.cone(vector, cone);
        Class<?> aClass = null;
        try {
            aClass = Class.forName("org.bukkit.entity." + this.projectile);
            if (Projectile.class.isAssignableFrom(aClass)) {
                Projectile projectile = mobEntity.launchProjectile(((Class<? extends Projectile>) aClass), vector);
                if (mobEntity instanceof Shulker) {
                    projectile.teleport(projectile.getLocation().add(vector.normalize().multiply(0.5)));
                }
                projectile.setGravity(gravity);
                projectile.setMetadata(INF_PROJECTILE_KEY, new LazyMetadataValue(InfPlugin.plugin, () -> damage));
                Utils.removeEntityLater(projectile, (int) Math.ceil((range / Math.max(0.01, speed)) * 20));
                return projectile;
            }
            Bukkit.getLogger().log(Level.WARNING, "no projectile fileName " + projectile);
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.WARNING, "no projectile fileName " + projectile);
        }
        return null;
    }

    @Override
    public String getName() {
        return "Projectile";
    }

//    @Override
//    public void onAttack(IMob mob, LivingEntity target) {
//        if (!Utils.possibility(onPlayerAttackChance)) return;
//
//        double speed = this.speed;
//        if (speed <= 0.1) speed = 0.1;
//
//        LivingEntity mobEntity = mob.getEntity();
//        Vector vector = Utils.unitDirectionVector(mobEntity.getEyeLocation().toVector(), target.getEyeLocation().toVector())
//                .multiply(speed);
//        int i = 0;
//        do {
//            new BukkitRunnable() {
//                @Override
//                public void run() {
//                    launch(mobEntity, vector, damageAmplifier*mob.getDamage());
//                }
//            }.runTaskLater(InfPlugin.plugin, burstInterval * i);
//        } while (++i < burstCount);
//    }
}
