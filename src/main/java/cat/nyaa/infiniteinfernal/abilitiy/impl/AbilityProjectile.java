package cat.nyaa.infiniteinfernal.abilitiy.impl;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.abilitiy.AbilityAttack;
import cat.nyaa.infiniteinfernal.abilitiy.AbilityTick;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbilityProjectile extends BaseAbility implements AbilityTick, AbilityAttack {
    private static final Vector x_axis = new Vector(1, 0, 0);
    private static final Vector y_axis = new Vector(0, 1, 0);
    private static final Vector z_axis = new Vector(0, 0, 1);

    @Serializable
    public int range = 30;
    @Serializable
    public double perCycleChance = 0.5;
    @Serializable
    public double onPlayerAttackChance = 1;
    @Serializable
    public double speed = 1;
    @Serializable
    public Projectile projectile;
    @Serializable
    public double cone = 0;
    @Serializable
    public boolean gravity = true;
    @Serializable
    public int burstCount = 1;
    @Serializable
    public int burstInterval = 10;

    private Projectile launch(LivingEntity mobEntity, Entity target, Vector vector, boolean isExtra) {
        vector = addCone(vector);
        Projectile projectile = mobEntity.launchProjectile(this.projectile.getClass(), vector);
        projectile.setGravity(gravity);
        Utils.removeEntityLater(projectile, (int) Math.ceil((range / Math.min(0.01, speed)) * 20));
        return projectile;
    }


    @Override
    public void tick(IMob iMob) {
        if (!Utils.possibility(perCycleChance)) return;
        LivingEntity mobEntity = iMob.getEntity();
        Stream<LivingEntity> validTarget = Utils.getValidTarget(iMob, iMob.getEntity().getNearbyEntities(range, range, range));
        LivingEntity target = Utils.randomPick(validTarget.collect(Collectors.toList()));
        if (target == null) return;
        if (!mobEntity.hasLineOfSight(target)) return;
        Vector vector = Utils.unitDirectionVector(mobEntity.getEyeLocation().toVector(), target.getEyeLocation().toVector())
                .multiply(speed);
        int i = 0;
        do {
            launch(mobEntity, target, vector, false);
        } while (++i < burstInterval);
    }

    private Vector addCone(Vector vector) {
        double phi = Utils.random() * 360;
        double theta = Utils.random() * cone;
        Vector clone = vector.clone();
        Vector crossP;

        if (clone.length() == 0) return vector;

        if (clone.getX() != 0 && clone.getZ() != 0) {
            crossP = clone.getCrossProduct(y_axis);
        } else if (clone.getX() != 0 && clone.getY() != 0) {
            crossP = clone.getCrossProduct(z_axis);
        } else {
            crossP = clone.getCrossProduct(x_axis);
        }
        crossP.normalize();

        clone.add(crossP.multiply(Math.tan(Math.toRadians(theta))));
        clone.rotateAroundNonUnitAxis(vector, Math.toRadians(phi));
        return clone;
    }

    @Override
    public String getName() {
        return "Projectile";
    }

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (!Utils.possibility(onPlayerAttackChance)) return;

        double speed = this.speed;
        if (speed <= 0.1) speed = 0.1;

        LivingEntity mobEntity = mob.getEntity();
        Vector vector = Utils.unitDirectionVector(mobEntity.getEyeLocation().toVector(), target.getEyeLocation().toVector())
                .multiply(speed);
        int i = 0;
        do {
            new BukkitRunnable() {
                @Override
                public void run() {
                    launch(mobEntity, target, vector, false);
                }
            }.runTaskLater(InfPlugin.plugin, burstInterval * i);
        } while (++i < burstCount);
    }
}
