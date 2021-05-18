package cat.nyaa.infiniteinfernal.mob.ability.impl.passive;

import cat.nyaa.infiniteinfernal.event.MobCastEvent;
import cat.nyaa.infiniteinfernal.mob.ability.api.AbilityAttack;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Collection;

public class AbilityPotionHit extends AbilityPassive implements AbilityAttack {
    @Serializable
    public String effect = "HARM";
    @Serializable
    public int duration = 1;
    @Serializable
    public int amplifier = 1;

    @Override
    public String getName() {
        return "PotionHit";
    }

    @Override
    public void onAttack(IMob mob, LivingEntity target) {
        Utils.doEffect(effect, target, duration, amplifier, getName());
    }

    @Override
    public void fire(IMob mob, MobCastEvent event) {
        Location selectedLocation = event.getSelectedLocation();
        Vector selectedVector = event.getSelectedVector();
        World world = selectedLocation.getWorld();
        if (world == null){
            return;
        }
        double length = selectedVector.length();
        world.getNearbyEntities(selectedLocation, length, length, length).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> ((LivingEntity) entity))
                .forEach(livingEntity -> onAttack(mob, livingEntity));

    }
}
