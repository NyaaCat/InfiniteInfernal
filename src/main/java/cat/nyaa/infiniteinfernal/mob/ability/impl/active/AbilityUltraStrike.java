package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.LocationUtil;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AbilityUltraStrike extends ActiveAbility {

    @Serializable
    public int amount = 5;

    @Serializable
    public int explodeRange = 3;

    @Serializable
    public int nearbyRange = 30;

    @Serializable
    public double damageMultiplier = 20;

    @Serializable
    public int delay = 60;

    @Override
    public void active(IMob iMob) {
        Queue<Entity> queue = new LinkedList<>(Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(nearbyRange, nearbyRange, nearbyRange)).collect(Collectors.toList()));

        for (int i = 0; i < amount; i++) {
            Entity poll = queue.poll();
            if (poll != null) {
                Location location = poll.getLocation();
                if (!iMob.getEntity().hasLineOfSight(poll)) {
                    i--;
                    continue;
                }
                summonUltraStrike(location, iMob);
            } else {
                summonUltraStrike(LocationUtil.randomNonNullLocation(iMob.getEntity().getLocation(), 0, nearbyRange), iMob);
            }
        }
    }

    private void summonUltraStrike(Location location, IMob iMob) {
        try {
            showEffect(location, iMob);
            Bukkit.getScheduler().runTaskLater(InfPlugin.plugin, () -> {
                boom(location, iMob);
            }, delay);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "config error");
        }
    }

    private void showEffect(Location location, IMob mobEntity) {
        final World world = location.getWorld();
        if (world == null)return;
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            int times = 0;
            @Override
            public void run() {
                if (times++ > 20){
                    this.cancel();
                    return;
                }
                world.spawnParticle(Particle.END_ROD, location, 20, 0, 0, 0, 0.03, null, true);
                world.spawnParticle(Particle.PORTAL, location, 30, 0, 0, 0, 2, null, true);
            }
        };
        bukkitRunnable.runTaskTimer(InfPlugin.plugin, 0, 1);

        for (int i = 0; i < 2; i++) {
            Bukkit.getScheduler().runTaskLater(InfPlugin.plugin, () -> {
                world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
            }, i * (delay / 2));
        }
    }

    private void boom(Location location, IMob iMob) {
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 1.5f);
        location.getWorld().spawnParticle(Particle.FLAME, location, 100, 0, 0, 0, 1, null, false);
        location.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, 100, 0, 0, 0, 1, null, false);
        Utils.getValidTargets(iMob, location.getWorld().getNearbyEntities(location, explodeRange, explodeRange, explodeRange))
                .forEach(entity -> {
                    if (!entity.equals(iMob)) {
                        double damage = iMob.getDamage();

                        double distance = entity.getLocation().distance(location);
                        double v = distance / explodeRange;
                        if (v > 1) return;
                        double distanceCorrection = Math.pow(v, 3);

                        damage = damage * this.damageMultiplier * (1 - distanceCorrection);

                        damage = Math.max(0, damage);
                        entity.damage(damage, iMob.getEntity());
                    }
                });
    }

    @Override
    public String getName() {
        return "UltraStrike";
    }
}
