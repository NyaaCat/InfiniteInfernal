package cat.nyaa.infiniteinfernal.mob.ability.impl.condition;

import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.ability.condition.PlayerCondition;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class PlayerHealthCondition extends PlayerCondition {
    @Serializable
    double lowerBound = 0;
    @Serializable
    double upperBound = 20;
    @Serializable
    boolean isPercentile = false;

    @Override
    public boolean check(IMob mob) {
        return getValidTargets(mob).anyMatch(this::checkHealth);
    }

    private boolean checkHealth(LivingEntity livingEntity) {
        double health = livingEntity.getHealth();
        AttributeInstance maxHealthAttr = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttr.getValue();
        if (isPercentile){
            double percentile = (health / maxHealth) * 100;
            return percentile > lowerBound && percentile <= upperBound;
        }
        return health > lowerBound && health <= upperBound;
    }
}
