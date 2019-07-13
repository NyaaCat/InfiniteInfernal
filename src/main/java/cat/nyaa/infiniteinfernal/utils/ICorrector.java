package cat.nyaa.infiniteinfernal.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public interface ICorrector {
    double getCorrection(LivingEntity entity, ItemStack itemStack);
}
