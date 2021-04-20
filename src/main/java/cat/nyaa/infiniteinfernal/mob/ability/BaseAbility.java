package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseAbility implements IAbility {

    protected List<LivingEntity> getNearbyEntities(IMob iMob, double radius){
        LivingEntity entity = iMob.getEntity();
        double extendedRange = radius * 1.5d;
        List<Entity> nearbyEntities = entity.getNearbyEntities(extendedRange, extendedRange, extendedRange);
        return nearbyEntities.stream().filter(entity1 -> entity1 instanceof LivingEntity && entity1.getLocation().distance(entity.getLocation()) <= radius)
                .map(entity1 -> ((LivingEntity) entity1))
                .filter(livingEntity -> !(livingEntity instanceof Player) || Utils.validGamemode((Player) livingEntity))
                .collect(Collectors.toList());
    }

    protected List<Player> getNearbyPlayers(IMob iMob, double radius){
        LivingEntity entity = iMob.getEntity();
        double extendedRange = radius * 1.5d;
        List<Entity> nearbyEntities = entity.getNearbyEntities(extendedRange, extendedRange, extendedRange);
        return nearbyEntities.stream().filter(entity1 -> entity1 instanceof Player && entity1.getLocation().distance(entity.getLocation()) <= radius)
                .map(entity1 -> ((Player) entity1))
                .filter(Utils::validGamemode)
                .collect(Collectors.toList());
    }
}
