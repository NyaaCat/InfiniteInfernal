package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.LivingEntity;

public interface IMessager {
    void broadcastToWorld(IMob deadMob, LivingEntity killer, ILootItem item);
    void broadcastExtraToWorld(IMob deadMob, LivingEntity killer, ILootItem item);
}
