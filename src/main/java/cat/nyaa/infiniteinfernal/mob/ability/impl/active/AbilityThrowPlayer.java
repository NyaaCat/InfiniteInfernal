package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;


public class AbilityThrowPlayer extends ActiveAbility implements AbilityAttack {
    @Serializable
    public double tickChance = 0.1;
    @Serializable
    public double attackChance = 0.2;
    @Serializable
    public double speed = 1;
    @Serializable
    public double range = 10;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (!RandomUtil.possibility(attackChance))return;
        new BukkitRunnable() {
            @Override
            public void run() {
                launch(target, mob);
            }
        }.runTaskLater(InfPlugin.plugin, 1);
    }

    @Override
    public void active(IMob iMob) {
        if (!RandomUtil.possibility(tickChance))return;

        LivingEntity mobEntity = iMob.getEntity();
        double bigRange = this.range*1.5;
        List<Entity> nearbyEntities = mobEntity.getNearbyEntities(bigRange, bigRange, bigRange);
        Utils.getValidTargets(iMob, nearbyEntities)
                .forEach(p -> launch(p, iMob));
    }

    private void launch(LivingEntity p, IMob iMob) {
        final Vector v = toVector((RandomUtil.random()*360) - 180, 45, speed);
        final LivingEntity player = p;
        if (player.getLocation().distance(iMob.getEntity().getLocation()) <= range) {
            player.setVelocity(v);
        }
    }

    private static Vector toVector(double yaw, double pitch, double length) {
        return new Vector(
                Math.cos(yaw / 180 * Math.PI) * Math.cos(pitch / 180 * Math.PI) * length,
                Math.sin(pitch / 180 * Math.PI) * length,
                Math.sin(yaw / 180 * Math.PI) * Math.cos(pitch / 180 * Math.PI) * length
        );
    }

    @Override
    public String getName() {
        return "ThrowPlayer";
    }
}
