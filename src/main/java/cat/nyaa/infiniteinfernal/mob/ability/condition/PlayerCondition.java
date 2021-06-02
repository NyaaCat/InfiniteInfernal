package cat.nyaa.infiniteinfernal.mob.ability.condition;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.stream.Stream;

public abstract class PlayerCondition extends BaseCondition{
    @Serializable
    double range = 15;

    public Stream<LivingEntity> getValidTargets(IMob mob){
        List<Entity> nearbyEntities = mob.getEntity().getNearbyEntities(range, range, range);
        return Utils.getValidTargets(mob, nearbyEntities).filter(livingEntity -> {
            World world = livingEntity.getWorld();
            if (world.equals(mob.getEntity().getWorld())){
                return livingEntity.getLocation().distance(mob.getEntity().getLocation()) <= range;
            }else return false;
        });
    }
}
