package cat.nyaa.infiniteinfernal.mob.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import jdk.internal.jline.internal.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AbilityMeteor extends ActiveAbility {
    @Serializable
    public int delay = 0;
    @Serializable
    public int amount = 10;
    @Serializable
    public double power = 1;

    @Override
    public void active(IMob iMob) {
        Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(30, 30, 30))
                .forEach(entity -> {
                    Location location = entity.getLocation().clone();
                    launch(iMob, entity, location);
                });

    }

    private void launch(IMob iMob, LivingEntity entity, Location location) {
        Location clone = location.clone();
        clone.add(0, 30, 0);
        clone.setPitch(90);
        ArmorStand armorStand = clone.getWorld().spawn(clone, ArmorStand.class, (e) -> {
            e.setVisible(false);
            e.setPersistent(false);
            e.setCanPickupItems(false);
            e.setGlowing(false);
            e.setBasePlate(false);
            e.setArms(false);
            e.setMarker(true);
            e.setInvulnerable(true);
            e.setGravity(false);
            e.setCollidable(false);
            e.setCustomName(iMob.getEntity().getCustomName());
        });
        Utils.removeEntityLater(armorStand, 100);
        for (int i = 0; i < amount; i++) {
            launchProjectile(iMob.getEntity(), entity, location, armorStand, i == 0 ? 0 : 15);
        }
    }


    protected Projectile launchProjectile(LivingEntity mobEntity, @Nullable Entity target, Location targetLocation, ArmorStand armorStand, double cone) {
        Vector fallVector = Utils.unitDirectionVector(armorStand.getLocation().toVector(), targetLocation.toVector());
        fallVector.multiply(power);
        fallVector = Utils.cone(fallVector, cone);
        if (target == null || armorStand.hasLineOfSight(target)) {
            Projectile projectile = armorStand.launchProjectile(Fireball.class, fallVector);
            projectile.setVelocity(fallVector);
            projectile.setShooter(mobEntity);
            projectile.setGravity(false);
            return projectile;
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return "Meteor";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        Location selectedLocation = event.getSelectedLocation();
        launch(mob, null, selectedLocation);
    }
}
