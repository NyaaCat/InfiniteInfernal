package cat.nyaa.infiniteinfernal.mob.controller;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.LivingEntity;

public interface IAggroControler {
    LivingEntity findAggroTarget(IMob iMob);
}
