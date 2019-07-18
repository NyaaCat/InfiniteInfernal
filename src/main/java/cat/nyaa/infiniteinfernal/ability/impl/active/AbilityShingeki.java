package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class AbilityShingeki extends ActiveAbility {
    @Serializable
    public int amount = 5;

    @Serializable
    public int delay = 10;

    @Serializable
    public double damageAmplifier = 2.0;

    public double radius = 3;

    @Override
    public void active(IMob iMob) {
        Queue<Entity> queue = new LinkedList<>(Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(30, 30, 30)).collect(Collectors.toList()));

        for (int i = 0; i < amount; i++) {
            Entity poll = queue.poll();
            if (poll != null) {
                Location location = poll.getLocation();
                if (!iMob.getEntity().hasLineOfSight(poll)) {
                    i--;
                    continue;
                }
                strike(location, iMob);
            } else {
                strike(Utils.randomSpawnLocation(iMob.getEntity().getLocation(), 0, 30), iMob);
            }
        }
    }

    private void strike(Location location, IMob iMob) {
        World world = location.getWorld();
        showEffect(location);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (world != null) {
                    Collection<Entity> nearbyEntities = world.getNearbyEntities(location, 3, 3, 3);
                    if (!nearbyEntities.isEmpty()) {
                        nearbyEntities.forEach(entity -> {
                            if (entity instanceof LivingEntity) {
                                double distance = entity.getLocation().distance(location);
                                double v = distance / radius;
                                if (v > 1) return;
                                double distanceCorrection = Math.pow(v, 3);
                                ((LivingEntity) entity).damage(iMob.getDamage() * damageAmplifier * (1 - distanceCorrection), iMob.getEntity());
                            }
                        });
                    }
                }
            }
        }.runTaskLater(InfPlugin.plugin, delay);
    }

    private void showEffect(Location location) {
        new BukkitRunnable(){
            @Override
            public void run() {
                World world = location.getWorld();
                if (world == null)return;
                List<Location> roundLocationList = Utils.getRoundLocations(location, radius);
                double theta = Utils.random() * 360;
                Vector vector = new Vector(1,0,0);
                vector.rotateAroundY(theta);
                List<Location> anchors = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    Location anchor = location.clone().add(vector);
                    vector.rotateAroundY(Math.toRadians(60));
                    anchors.add(anchor);
                }

                roundLocationList.addAll(Utils.drawHexStar(anchors));

                int tasks = (int) (Math.round((double) roundLocationList.size()) / ((double) delay));
                int stepsPerTask = (int) (Math.round((double) roundLocationList.size()) / ((double) tasks));

                for (int i = 0; i < tasks; i++) {
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            for (int j = 0; j < stepsPerTask; j++) {
                                world.spawnParticle(Particle.END_ROD,roundLocationList.get(j), 1, 0.2, 0.2, 0.2, 0, null, true);
                            }
                        }
                    }.runTaskLater(InfPlugin.plugin, Math.round(((double) delay) / ((double) tasks)));
                }
            }
        }.runTaskAsynchronously(InfPlugin.plugin);
    }

    @Override
    public String getName() {
        return "Shingeki";
    }
}
