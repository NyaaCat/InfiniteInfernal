package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
        PotionEffectType effectType = PotionEffectType.getByName(this.effect);
        PotionEffect effect = effectType.createEffect(duration,amplifier);
        entityStream.forEach(livingEntity -> {
            PotionEffect potionEffect = livingEntity.getPotionEffect(effectType);
            if (potionEffect != null && potionEffect.getAmplifier()>amplifier){
                return;
            }
            livingEntity.removePotionEffect(effectType);
            livingEntity.addPotionEffect(effect, true);
        });
    }

    @Override
    public String getName() {
        return "AoePotion";
    }
}
