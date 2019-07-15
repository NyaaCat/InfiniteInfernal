package cat.nyaa.infiniteinfernal.controler;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.LivingEntity;

public interface IAggroControler {
    LivingEntity findAggroTarget(IMob iMob);
}
