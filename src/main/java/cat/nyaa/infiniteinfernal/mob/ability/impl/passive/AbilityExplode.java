package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityDeath;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDeathEvent;

public class AbilityExplode extends AbilityPassive implements AbilityDeath {
    @Serializable
    public double power = 1;

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        Location location = iMob.getEntity().getLocation();
        World world = location.getWorld();
        if (world != null) {
            world.createExplosion(location.getX(), location.getY(), location.getZ(), ((float) power), false, false);
        }
    }

    @Override
    public String getName() {
        return "Explode";
    }
}
