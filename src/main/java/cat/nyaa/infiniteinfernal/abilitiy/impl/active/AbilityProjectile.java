package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.abilitiy.AbilityAttack;
import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbilityProjectile extends ActiveAbility implements AbilityAttack {

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
        vector = Utils.cone(vector, cone);
        Projectile projectile = mobEntity.launchProjectile(this.projectile.getClass(), vector);
        projectile.setGravity(gravity);
        Utils.removeEntityLater(projectile, (int) Math.ceil((range / Math.min(0.01, speed)) * 20));
        return projectile;
    }


    @Override
    public void active(IMob iMob) {
        if (!Utils.possibility(perCycleChance)) return;
        LivingEntity mobEntity = iMob.getEntity();
        Stream<LivingEntity> validTarget = Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(range, range, range));
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
