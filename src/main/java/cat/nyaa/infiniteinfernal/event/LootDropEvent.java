package cat.nyaa.infiniteinfernal.event;

import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDeathEvent;

public class LootDropEvent extends Event {
    private final Player killer;
    private final IMob iMob;
    private ILootItem loot;
    private ILootItem specialLoot;
    private EntityDeathEvent ev;
    public static final HandlerList handlerList = new HandlerList();

    public ILootItem getLoot() {
        return loot;
    }

    public ILootItem getSpecialLoot() {
        return specialLoot;
    }

    public void setLoot(ILootItem loot) {
        this.loot = loot;
    }

    public void setSpecialLoot(ILootItem specialLoot) {
        this.specialLoot = specialLoot;
    }

    public Player getKiller() {
        return killer;
    }

    public IMob getiMob() {
        return iMob;
    }

    public LootDropEvent(Player killer, IMob iMob, ILootItem loot, ILootItem specialLoot, EntityDeathEvent ev) {
        this.killer = killer;
        this.iMob = iMob;
        this.loot = loot;
        this.specialLoot = specialLoot;
        this.ev = ev;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public EntityDeathEvent getEntityDeathEvent() {
        return ev;
    }
}
