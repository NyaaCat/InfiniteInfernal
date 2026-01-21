package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.LivingEntity;

import java.util.stream.Stream;

public class AbilityAoePotion extends ActiveAbility {
    @Serializable
    public String effect = "SLOW";
    @Serializable
    public int duration = 10;
    @Serializable
    public int amplifier = 1;
    @Serializable
    public double radius = 25;

    @Override
    public void active(IMob iMob) {
        Stream<LivingEntity> entityStream = Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(radius,radius,radius));
        entityStream.forEach(livingEntity -> Utils.doEffect(effect, livingEntity, duration, amplifier, getName()));
    }

    @Override
    public String getName() {
        return "AoePotion";
    }
}
