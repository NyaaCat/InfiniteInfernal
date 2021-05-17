package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.RandomUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class AbilityIgnite  extends AbilityPassive implements AbilityAttack {

    @Serializable
    public int duration = 40;
    @Serializable
    public double chance = 1d;

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        if (RandomUtil.possibility(chance)){
            target.setFireTicks(duration);
        }
    }

    @Override
    public String getName() {
        return "ignite";
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        Location selectedLocation = event.getSelectedLocation();
        World world = selectedLocation.getWorld();
        if(world == null){
            return;
        }

        Vector selectedVector = event.getSelectedVector();
        double radius = selectedVector.length();
        world.getNearbyEntities(selectedLocation, radius, radius, radius)
                .stream().forEach(entity -> {
                    entity.setFireTicks(duration);
                });
    }
}
