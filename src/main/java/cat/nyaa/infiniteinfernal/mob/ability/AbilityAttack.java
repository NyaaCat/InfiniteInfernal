package cat.nyaa.infiniteinfernal.mob.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.LivingEntity;

public interface AbilityAttack {
    void onAttack(IMob mob, LivingEntity target);
}
