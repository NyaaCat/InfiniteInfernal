package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AbilityLifesteal extends ActiveAbility {
    @Serializable
    public double suck = 10;
    @Serializable
    public double gain = 10;
    @Serializable
    public double range = 20;

    @Override
    public void active(IMob iMob) {
        LivingEntity target = Utils.randomPick(Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(20, 20, 20))
                .filter(entity -> entity.getEyeLocation().distance(iMob.getEntity().getEyeLocation()) < range)
                .collect(Collectors.toList())
        );
        if (target != null) {
            fire(iMob, target);
        }
    }

    private void fire(IMob mob, LivingEntity target) {
//        EntityDamageByEntityEvent ev = new EntityDamageByEntityEvent(mob.getEntity(), target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, mob.getDamage());
//        Bukkit.getPluginManager().callEvent(ev);
//        if (ev.isCancelled()){
//            return;
//        }
//        double finalDamage = ev.getFinalDamage();
        double health = mob.getEntity().getHealth();
        double max = mob.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double regen = max * (gain / 100d);
        mob.getEntity().setHealth(Math.max(0, Math.min(health + regen, max)));
        target.setHealth(Math.max(0.1, target.getHealth() - suck));
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMITE_AMBIENT, 1f, 0.5f);
        new LifestealEffect(target, mob).runTask(InfPlugin.plugin);
    }

    @Override
    public String getName() {
        return "LifeSteal";
    }

    private class LifestealEffect extends BukkitRunnable {
        double totalLength;
        Location end;
        Location current;
        Particle particle = Particle.HEART;
        World world;
        LivingEntity endEntity;
        LivingEntity fromEntity;

        public LifestealEffect(LivingEntity target, IMob mob) {
            totalLength = target.getLocation().distance(mob.getEntity().getLocation());
            current = target.getEyeLocation();
            end = mob.getEntity().getEyeLocation();
            world = target.getWorld();
            endEntity = mob.getEntity();
            fromEntity = target;
        }

        @Override
        public void run() {
            if (totalLength < 2) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location towards = end.clone().subtract(current).multiply(0.1);
                        for (int i = 0; i < 10; i++) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Location clone = current.clone();
                                    clone.add(towards);
                                    spawnParticle(towards, particle);
                                    current = clone;
                                }
                            }.runTaskLater(InfPlugin.plugin, i);
                        }
                    }
                }.runTaskAsynchronously(InfPlugin.plugin);
            } else {
                new BukkitRunnable() {
                    AtomicDouble remains = new AtomicDouble(0);

                    @Override
                    public void run() {
                        Location eyeLocation = endEntity.getEyeLocation();
                        Location fromLocation = fromEntity.getEyeLocation();
                        if (Objects.equals(eyeLocation.getWorld(), fromLocation.getWorld())) {
                            totalLength = eyeLocation.distance(fromLocation);
                        }
                        end = eyeLocation;
                        double stepLength = 0.5;
                        Vector direction = end.clone().subtract(current).toVector().normalize().multiply(stepLength);
                        double distance = end.distance(current);
                        if (distance < 0.5) {
                            this.cancel();
                            return;
                        }
                        double x = distance / totalLength;
                        double lengthInTick = (speedShift(x) / 20) + remains.getAndSet(0);
                        while ((lengthInTick -= stepLength) >= 0) {
                            distance = end.distance(current);
                            if (distance < 0.5) {
                                this.cancel();
                                return;
                            }
                            x = distance / totalLength;
                            spawnStarParticle(current, direction, x);
                            current.add(direction);
                        }
                        remains.set(lengthInTick + stepLength);
                    }
                }.runTaskTimer(InfPlugin.plugin, 0, 1);
            }
        }

        private void spawnParticle(Location location, Particle particle) {
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, 1, 0, 0, 0, 0, null, true);
            }
        }

        Vector yAxies = new Vector(0, 1, 0);
        Vector xAxies = new Vector(1, 0, 0);

        private void spawnStarParticle(Location location, Vector towards, double x) {
            Vector nonLinerVec;
            if (towards.getX() != 0 || towards.getZ() != 0) {
                nonLinerVec = yAxies;
            } else if (towards.getY() != 0) {
                nonLinerVec = xAxies;
            } else throw new IllegalArgumentException("towards 0");
            Vector crossProduct = towards.getCrossProduct(nonLinerVec);
            Vector v1 = crossProduct.getCrossProduct(towards).normalize().multiply(3 * (distanceShift(x)));
            Vector v2 = v1.clone().rotateAroundAxis(towards, Math.toRadians(72));
            Vector v3 = v2.clone().rotateAroundAxis(towards, Math.toRadians(72));
            Vector v4 = v3.clone().rotateAroundAxis(towards, Math.toRadians(72));
            Vector v5 = v4.clone().rotateAroundAxis(towards, Math.toRadians(72));
            spawnParticle(location.clone().add(v1), particle);
            spawnParticle(location.clone().add(v2), particle);
            spawnParticle(location.clone().add(v3), particle);
            spawnParticle(location.clone().add(v4), particle);
            spawnParticle(location.clone().add(v5), particle);
        }

        private double distanceShift(double x) {
            //0.57x^2-3,89x^3+10.88x^4-7.56x^5
            return 0.57 * Math.pow(x, 2) - 3.89 * Math.pow(x, 3) + 10.88 * Math.pow(x, 4) - 7.56 * Math.pow(x, 5);
        }

        private double speedShift(double x) {
            return Math.pow(x, 4) * (-15) + 30;
        }
    }
}
