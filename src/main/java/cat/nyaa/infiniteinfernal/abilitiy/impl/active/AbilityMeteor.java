package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.*;
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
                    location.add(0, 30, 0);
                    location.setPitch(90);
                    ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class, (e) -> {
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
                    for (int i = 0; i < amount; i++) {
                        launch(iMob.getEntity(), entity, armorStand, i == 0 ? 0 : 15);
                    }
                });

    }


    protected Projectile launch(LivingEntity mobEntity, Entity target, ArmorStand armorStand, double cone) {
        Utils.removeEntityLater(armorStand, 100);
        Vector fallVector = Utils.unitDirectionVector(armorStand.getLocation().toVector(), target.getLocation().toVector());
        fallVector.multiply(power);
        if (armorStand.hasLineOfSight(target)) {
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
}
