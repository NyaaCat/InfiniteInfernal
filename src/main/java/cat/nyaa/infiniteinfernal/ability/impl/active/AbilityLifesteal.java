package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.AbilityAttack;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
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

public class AbilityLifesteal extends ActiveAbility {
    @Serializable
    public double suck = 10;
    @Serializable
    public double gain = 10;

    @Override
    public void active(IMob iMob) {
        LivingEntity target = iMob.getTarget();
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
        Particle particle = Particle.DAMAGE_INDICATOR;
        World world;

        public LifestealEffect(LivingEntity target, IMob mob) {
            totalLength = target.getLocation().distance(mob.getEntity().getLocation());
            current = target.getEyeLocation();
            end = mob.getEntity().getEyeLocation();
            world = target.getWorld();
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
                    @Override
                    public void run() {

                    }
                }.runTaskTimer(InfPlugin.plugin, 0, 1);
            }
        }

        private void spawnParticle(Location location, Particle particle) {
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, 1, 0.05d, 0.05d, 0.05d, 0, null, true);
            }
        }

        private double distanceShift(double distance) {
            //0.57x^2-3,89x^3+10.88x^4-7.56x^5
            double x = (distance / totalLength);
            return 0.57*Math.pow(x,2) - 3.89*Math.pow(x,3) + 10.88*Math.pow(x,4) - 7.56*Math.pow(x,5);
        }

        private double speedShift(double distance) {
            double x = (distance / totalLength);
            return x * x * 9.5 + 0.5;
        }
    }
}
