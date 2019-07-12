package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.stream.Stream;

public class AbilityAoePotion extends ActiveAbility {
    @Serializable
    public PotionEffectType effect = PotionEffectType.SLOW;
    @Serializable
    public int duration = 10;
    @Serializable
    public int amplifier = 1;
    @Serializable
    public double radius = 25;

    @Override
    public void active(IMob iMob) {
        Stream<LivingEntity> entityStream = Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(radius,radius,radius));
        PotionEffect effect = this.effect.createEffect(duration,amplifier);
        entityStream.forEach(livingEntity -> livingEntity.addPotionEffect(effect));
    }

    @Override
    public String getName() {
        return "AoePotion";
    }
}
