package cat.nyaa.infiniteinfernal.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class CorrectorAttribute implements ICorrector {
    private final Attribute attribute;
    private final double amplifier;

    CorrectorAttribute(Attribute attribute, double amplifier){
        this.attribute = attribute;
        this.amplifier = amplifier;
    }

    @Override
    public double getCorrection(LivingEntity entity, ItemStack itemStack) {
        AttributeInstance attribute = entity.getAttribute(this.attribute);
        if (attribute != null){
            return attribute.getValue() * amplifier;
        }
        else return 0;
    }
}
