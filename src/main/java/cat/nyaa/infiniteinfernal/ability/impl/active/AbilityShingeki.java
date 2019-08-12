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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AbilityShingeki extends ActiveAbility {
    @Serializable
    public int amount = 5;

    @Serializable
    public int delay = 10;

    @Serializable
    public double damageAmplifier = 2.0;

    public double radius = 5;

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
                Location location = null;
                for (int j = 0; j < 20; j++) {
                    location = Utils.randomSpawnLocation(iMob.getEntity().getLocation(), 0, 30);
                    if (location != null) break;
                }
                if (location != null) {
                    strike(location, iMob);
                }
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
                    for (int i = 0; i < 5; i++) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                world.strikeLightningEffect(location);
                            }
                        }.runTaskLater(InfPlugin.plugin, i * 4);
                    }
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
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = location.getWorld();
                if (world == null) return;
                List<Location> roundLocationList = Utils.getRoundLocations(location, radius);
                double theta = Utils.random() * 360;
                Vector vector = new Vector(1, 0, 0);
                vector.multiply(radius);
                vector.rotateAroundY(theta);
                List<Location> anchors = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    Location anchor = location.clone().add(vector);
                    vector.rotateAroundY(Math.toRadians(60));
                    anchors.add(anchor);
                }

                List<Location> hexStar = Utils.drawHexStar(anchors);
                int sizePerTask = roundLocationList.size() / 3;
                List<Location> r1 = roundLocationList.subList(0, sizePerTask);
                List<Location> r2 = roundLocationList.subList(sizePerTask, 2 * sizePerTask);
                List<Location> r3 = roundLocationList.subList(2 * sizePerTask, roundLocationList.size());

                draw(hexStar, delay);
                draw(r1, delay);
                draw(r2, delay);
                draw(r3, delay);
            }

            private void draw(List<Location> locations, int delay) {
                double stepsPerTask = (double) locations.size() / (double) delay;
                World world = location.getWorld();
                if (world == null) return;
                AtomicInteger spawned = new AtomicInteger(0);
                AtomicInteger taskNum = new AtomicInteger(1);
                class Task extends BukkitRunnable {
                    @Override
                    public void run() {
                        while (spawned.get() < stepsPerTask * taskNum.get()) {
                            world.spawnParticle(Particle.END_ROD, locations.get(spawned.getAndAdd(1)), 1, 0, 0, 0, 0, null, true);
                        }
                        if (taskNum.getAndAdd(1) < delay) {
                            new Task().runTaskLater(InfPlugin.plugin, 1);
                        }
                    }
                }
                new Task().runTask(InfPlugin.plugin);
            }
        }.runTaskAsynchronously(InfPlugin.plugin);
    }

    @Override
    public String getName() {
        return "Shingeki";
    }
}
