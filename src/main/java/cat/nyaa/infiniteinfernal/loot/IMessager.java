package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.Entity;

public interface IMessager {
    void broadcastToWorld(IMob deadMob, Entity killer, ILootItem item);
    void broadcastExtraToWorld(IMob deadMob, Entity killer, ILootItem item);
}
