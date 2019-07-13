package cat.nyaa.infiniteinfernal.utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class CorrectorEnchant implements ICorrector {
    private final Enchantment enchantment;
    private final double amplifier;

    public CorrectorEnchant(Enchantment enchantment, double amplifier) {
        this.enchantment = enchantment;
        this.amplifier = amplifier;
    }

    @Override
    public double getCorrection(LivingEntity entity, ItemStack itemStack) {
        if (itemStack == null)return 0;
        int enchantmentLevel = itemStack.getEnchantmentLevel(enchantment);
        return enchantmentLevel * amplifier;
    }
}
