package cat.nyaa.infiniteinfernal.mob.ability.api;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.event.entity.EntityDeathEvent;

public interface AbilityDeath {
    void onMobDeath(IMob iMob, EntityDeathEvent ev);
}
