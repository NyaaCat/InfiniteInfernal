package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
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
    public int duration = 10;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (!Utils.possibility(attackChance))return;
        launch(target);
    }

    @Override
    public void active(IMob iMob) {
        if (!Utils.possibility(tickChance))return;

        LivingEntity mobEntity = iMob.getEntity();
        List<Entity> nearbyEntities = mobEntity.getNearbyEntities(20, 20, 20);
        Utils.getValidTargets(iMob, nearbyEntities)
                .forEach(this::launch);
    }

    private void launch(LivingEntity p) {
        Location direction = p.getEyeLocation();
        new BukkitRunnable() {
            private final static int ELYTRA_DELAY = 3;
            final int d = duration;
            final Vector v = toVector(direction.getYaw(), direction.getPitch(), speed);
            final LivingEntity player = p;
            int current = 0;
            boolean stopped = false;

            @Override
            public void run() {
                if (!stopped) {
                    if (current < d) {
                        current++;
                        player.setVelocity(v);
                    } else {
                        player.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(speed));
                        cancel();
                        stopped = true;
                    }
                } else {
                    cancel();
                    stopped = true;
                }
            }
        }.runTaskTimer(InfPlugin.plugin, 1, 1);
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
