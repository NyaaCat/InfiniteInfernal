package cat.nyaa.infiniteinfernal.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CorrectorEffect implements ICorrector {
    private final PotionEffectType type;
    private final double amplifier;

    CorrectorEffect(PotionEffectType type, double amplifier){
        this.type = type;
        this.amplifier = amplifier;
    }

    @Override
    public double getCorrection(LivingEntity entity, ItemStack itemStack) {
        PotionEffect potionEffect = entity.getPotionEffect(type);
        if (potionEffect != null){
            return potionEffect.getAmplifier() * amplifier;
        }
        return 0;
    }
}
