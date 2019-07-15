package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;

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
            }else {
                summonUltraStrike(Utils.randomSpawnLocation(iMob.getEntity().getLocation(), 0, nearbyRange), iMob);
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
        location.getWorld().spawnParticle(Particle.END_ROD, location, 5, 0, 0, 0, 0.01, null, true);
        location.getWorld().spawnParticle(Particle.PORTAL, location, 30, 0, 0, 0, 2, null, true);
        for (int i = 0; i < delay / 20; i++) {
            Bukkit.getScheduler().runTaskLater(InfPlugin.plugin, () -> {
                location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
            }, i * 20);
        }
    }

    private void boom(Location location, IMob iMob) {
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1, 1.5f);
        location.getWorld().spawnParticle(Particle.FLAME, location, 100, 1);
        location.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, 100, 1);
        Utils.getValidTargets(iMob, location.getWorld().getNearbyEntities(location, explodeRange, explodeRange, explodeRange))
                .forEach(entity -> {
                    if (!entity.equals(iMob)) {
                        double damage = iMob.getDamage();
                        damage = damage * this.damageMultiplier;
                        double distance = entity.getLocation().distance(location);
                        damage = Math.max(0, (1 - (distance / ((double) explodeRange))) * damage);
                        entity.damage(damage, iMob.getEntity());
                    }
                });
    }

    @Override
    public String getName() {
        return "UltraStrike";
    }
}
