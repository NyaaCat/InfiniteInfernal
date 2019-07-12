package cat.nyaa.infiniteinfernal.abilitiy.impl.passive;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityDeath;
import cat.nyaa.infiniteinfernal.abilitiy.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.entity.EntityDeathEvent;

public class AbilityExplode extends AbilityPassive implements AbilityDeath {
    @Serializable
    public float power = 1;

    @Override
    public void onMobDeath(IMob iMob, EntityDeathEvent ev) {
        Location location = iMob.getEntity().getLocation();
        World world = location.getWorld();
        if (world != null) {
            world.createExplosion(location, power);
        }
    }

    @Override
    public String getName() {
        return "Explode";
    }
}
