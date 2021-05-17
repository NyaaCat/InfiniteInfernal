package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityDeath;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class AbilityExplode extends AbilityPassive implements AbilityDeath {
    @Serializable
    public double power = 1;
    @Serializable
    public boolean vanilla = false;
    @Serializable
    public double damageOverride = 10;
    @Serializable
    public double rangeOverride = 100;

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        Location location = iMob.getEntity().getLocation();
        if(vanilla){
            explodeOn(location);
        }else {
            doExplode(location, rangeOverride, damageOverride);
        }
    }

    private void explodeOn(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.createExplosion(location.getX(), location.getY(), location.getZ(), ((float) power), false, false);
        }
    }

    private void doExplode(Location center, double range, double damage){
        World world = center.getWorld();
        if (world == null){
            return;
        }
        world.spawnParticle(Particle.EXPLOSION_LARGE, center, 100, range, range, range);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
        world.getNearbyEntities(center, range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> ((LivingEntity) entity))
                .forEach(livingEntity -> livingEntity.damage(damage));
    }

    @Override
    public String getName() {
        return "Explode";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        Location selectedLocation = event.getSelectedLocation();
        if(vanilla){
            explodeOn(selectedLocation);
        }else {
            doExplode(selectedLocation, rangeOverride, damageOverride);
        }
    }
}
